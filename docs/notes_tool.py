#!/usr/bin/env python3
"""Extract / apply reveal.js speaker notes for ai-architecture.html.

The deck stores each slide's presenter notes in an <aside class="notes">...</aside>
block. This tool round-trips those notes through an editable Markdown file:

    # Extract notes from the HTML into a Markdown file
    python3 notes_tool.py extract ai-architecture.html -o speaker-notes.md

    # Edit speaker-notes.md, then write the notes back into the HTML
    python3 notes_tool.py apply ai-architecture.html speaker-notes.md

Mapping is positional: the Nth "## Slide N" block in the Markdown maps to the
Nth <aside class="notes"> in the HTML. The number of blocks must match the number
of notes in the HTML, otherwise apply aborts without touching the file.

Note bodies are treated as raw HTML fragments (passthrough), so inline markup you
add in the Markdown is preserved. Paragraphs are separated by blank lines.
"""
import argparse
import html
import re
import sys

ASIDE_RE = re.compile(r'(<aside class="notes">)(.*?)(</aside>)', re.DOTALL)
SECTION_RE = re.compile(r'<section\b.*?</section>', re.DOTALL)
HEADING_RE = re.compile(r'<h[123][^>]*>(.*?)</h[123]>', re.DOTALL)

# Indentation used when writing notes back into the HTML (matches the file style).
PARA_INDENT = " " * 10
CLOSE_INDENT = " " * 8


def strip_tags(s):
    s = re.sub(r'<br\s*/?>', ' ', s)
    s = re.sub(r'<[^>]+>', '', s)
    s = html.unescape(s)
    return re.sub(r'\s+', ' ', s).strip()


def inner_to_paragraphs(inner):
    """Turn an <aside> inner fragment into a list of single-line paragraphs."""
    paras = []
    for chunk in re.split(r'\n\s*\n', inner.strip('\n')):
        text = re.sub(r'\s+', ' ', chunk).strip()
        if text:
            paras.append(text)
    return paras


def slide_titles(htext):
    """Return the heading text for each <section> that contains notes, in order."""
    titles = []
    for section in SECTION_RE.findall(htext):
        if '<aside class="notes">' not in section:
            continue
        m = HEADING_RE.search(section)
        titles.append(strip_tags(m.group(1)) if m else "(untitled)")
    return titles


def cmd_extract(args):
    htext = open(args.html, encoding="utf-8").read()
    titles = slide_titles(htext)
    notes = [inner_to_paragraphs(m.group(2)) for m in ASIDE_RE.finditer(htext)]

    if len(titles) != len(notes):
        print(f"warning: {len(titles)} headed sections but {len(notes)} note blocks",
              file=sys.stderr)

    out = ["# Speaker Notes — ai-architecture.html", ""]
    out.append("<!-- Edit note text freely. Keep each '## Slide N' header line intact —")
    out.append("     apply maps blocks to <aside class=\"notes\"> by order. -->")
    out.append("")
    for i, paras in enumerate(notes):
        title = titles[i] if i < len(titles) else ""
        header = f"## Slide {i + 1}" + (f" — {title}" if title else "")
        out.append(header)
        out.append("")
        out.extend(["\n".join(paras) if paras else "(no notes)", ""])

    with open(args.output, "w", encoding="utf-8") as f:
        f.write("\n".join(out).rstrip() + "\n")
    print(f"Extracted {len(notes)} note blocks -> {args.output}")


def parse_md(mdtext):
    """Return list of note bodies (each a list of paragraphs) from the Markdown."""
    blocks = []
    current = None
    for line in mdtext.splitlines():
        if re.match(r'^##\s+Slide\s+\d+', line):
            current = []
            blocks.append(current)
        elif current is not None:
            current.append(line)
    notes = []
    for body in blocks:
        text = "\n".join(body).strip()
        if text == "(no notes)":
            notes.append([])
            continue
        notes.append(inner_to_paragraphs(text))
    return notes


def cmd_apply(args):
    htext = open(args.html, encoding="utf-8").read()
    notes = parse_md(open(args.md, encoding="utf-8").read())
    aside_count = len(ASIDE_RE.findall(htext))

    if len(notes) != aside_count:
        sys.exit(f"abort: {len(notes)} slide blocks in {args.md} but "
                 f"{aside_count} note blocks in {args.html}. No changes made.")

    counter = {"i": 0}

    def repl(m):
        paras = notes[counter["i"]]
        counter["i"] += 1
        body = ("\n\n" + PARA_INDENT).join(paras)
        inner = f"\n{PARA_INDENT}{body}\n{CLOSE_INDENT}" if paras else "\n" + CLOSE_INDENT
        return m.group(1) + inner + m.group(3)

    new_html = ASIDE_RE.sub(repl, htext)

    if not args.dry_run:
        with open(args.html, "w", encoding="utf-8") as f:
            f.write(new_html)
        print(f"Applied {len(notes)} note blocks -> {args.html}")
    else:
        print(f"[dry-run] would update {len(notes)} note blocks in {args.html}")


def main():
    p = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="Run 'notes_tool.py extract -h' or 'notes_tool.py apply -h' "
               "for command-specific examples.")
    sub = p.add_subparsers(dest="cmd", required=True)

    pe = sub.add_parser(
        "extract", help="extract notes from HTML into Markdown",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        description="Read the <aside class=\"notes\"> blocks from the HTML and write "
                    "them to an editable Markdown file.",
        epilog="examples:\n"
               "  # default output (speaker-notes.md)\n"
               "  python3 notes_tool.py extract ai-architecture.html\n\n"
               "  # custom output file\n"
               "  python3 notes_tool.py extract ai-architecture.html -o notes.md")
    pe.add_argument("html", help="source reveal.js HTML file")
    pe.add_argument("-o", "--output", default="speaker-notes.md",
                    help="Markdown file to write (default: speaker-notes.md)")
    pe.set_defaults(func=cmd_extract)

    pa = sub.add_parser(
        "apply", help="write Markdown notes back into the HTML",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        description="Write the notes from the Markdown file back into the HTML, "
                    "mapping '## Slide N' blocks to <aside class=\"notes\"> by order.",
        epilog="examples:\n"
               "  # write edited notes back into the deck\n"
               "  python3 notes_tool.py apply ai-architecture.html speaker-notes.md\n\n"
               "  # preview the change without writing\n"
               "  python3 notes_tool.py apply ai-architecture.html speaker-notes.md --dry-run")
    pa.add_argument("html", help="target reveal.js HTML file (edited in place)")
    pa.add_argument("md", help="Markdown file produced by 'extract'")
    pa.add_argument("--dry-run", action="store_true",
                    help="report what would change without writing")
    pa.set_defaults(func=cmd_apply)

    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
