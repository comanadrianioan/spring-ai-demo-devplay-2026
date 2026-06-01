package com.spring_ai.devplay_2026.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ScheduleTools {

    @Tool(name = "DevPlaySchedule",
          description = "Full two-day schedule for Dev.Play 2026 (June 8–9, Bucharest). "
                      + "Returns session times, titles, speakers, descriptions, and registration requirements.")
    public String getDevPlaySchedule() {
        return """
                DEV.PLAY 2026 SCHEDULE
                June 8–9, Bucharest, Romania
                ==============================

                ── MONDAY, JUNE 8 ─────────────────────────────────
                MAIN STAGE

                10:00–10:45 | Welcome Coffee
                  Registration required: No

                10:45–11:00 | Opening Speech
                  Speakers: Andreea Sava & Catalin Butnariu (RGDA)
                  Description: Official conference welcome and opening remarks.
                  Registration required: No

                11:00–11:45 | KEYNOTE — 2026 Industry Update
                  Speaker: Shawn Foust (Fortis Games)
                  Description: An overview of the current state of the games industry, key trends, and what studios should watch in 2026.
                  Registration required: No

                11:45–12:15 | Understanding Suspense in Video Games
                  Speaker: Andrei Nae (University of Bucharest)
                  Description: Explores how "metasuspense" — rooted in postmodernist art — operates as a design tool in video games and shapes player experience.
                  Registration required: No

                12:15–13:00 | Perspectives Clash: Part 1
                  Speakers: Rami Ismail vs. Adam Boyes (moderated dialogue)
                  Description: Unscripted debate between two industry veterans covering development philosophy, publishing relationships, and long-term studio strategy.
                  Registration required: No

                13:00–14:00 | Lunch Break

                14:00–14:45 | Perspectives Clash: Part 2
                  Speakers: Rami Ismail vs. Adam Boyes
                  Description: Continuation of the moderated discussion with open audience participation.
                  Registration required: No

                14:45–15:15 | How to Market a Game When Everyone Knows How
                  Speaker: Cristian Dina (Amber)
                  Description: A candid post-mortem on marketing Mexican Ninja across Steam and consoles — what worked, what didn't, and why conventional wisdom has its limits.
                  Registration required: No

                15:15–15:45 | Future of Learning Through Play, Culture, Connection
                  Speaker: Curt Fortin (Tales of Us)
                  Description: How World of Us uses games to foster cross-cultural understanding and educational outcomes, and the business model behind it.
                  Registration required: No

                15:45–16:15 | Coffee Break

                16:15–16:45 | Permission Not Required – Outgrowing "File a Ticket and Wait"
                  Speaker: TBA
                  Description: How studios can build internal tools culture so developers solve problems themselves rather than waiting in queues.
                  Registration required: No

                16:45–17:45 | PANEL — The Struggle Is Real: Indie Life in Emerging Markets in 2026
                  Panelists: Jay Powell, Radu Ziemba, Theo Gavriilidis, Lara Davidova
                  Moderator: Bobby Wertheim
                  Description: A candid panel on the real challenges of making and releasing games as an indie in regions outside the traditional industry hubs.
                  Registration required: No

                21:00–02:00 | Dev.Play Official Party
                  Venue: Expirat Club, Bucharest (powered by Amber)
                  Description: Official evening celebration for all attendees.
                  Registration required: No

                WORKSHOP STAGE

                12:00–13:00 | Roundtable: The Role of Game Development Education in a Changing Industry
                  Description: Private roundtable session for invited participants.
                  Registration required: Private (invite only)

                14:00–15:00 | Student Workshop: Fail Fast, Play Faster — The Prototyping Journey in Game Design
                  Speaker: Stefan Zamfir (Echo School)
                  Description: Hands-on workshop guiding students through rapid game design prototyping methodology, embracing failure as a learning tool.
                  Registration required: Yes (registration form available on the website)

                16:15–16:45 | Roundtable: Eastern & Southern Europe Trade Associations Meetup
                  Description: Private meetup for regional trade association representatives.
                  Registration required: Private (invite only)

                ── TUESDAY, JUNE 9 ────────────────────────────────
                MAIN STAGE

                10:00–10:30 | Welcome Coffee
                  Registration required: No

                10:30–11:00 | AMA: Behind the Scenes of Hazelight's Development Process (vol. 2)
                  Speaker: Philip Martin (Hazelight)
                  Description: Open Q&A on team structure, creative decision-making, and production realities at the studio behind It Takes Two and Split Fiction.
                  Registration required: No

                11:00–11:30 | Is the Games Industry Broken?
                  Speaker: Eugen Sfirlos (Keywords Studios)
                  Description: Examines the current industry transformation, drawing comparisons with disruptions in film and music, and asks whether games are facing a structural crisis or a necessary reset.
                  Registration required: No

                11:30–12:15 | Seamless Transitions: From Gameplay to Cinematics
                  Speakers: Igor Sobolev & Theodore Hilhorst (Hangar 13 / 2K)
                  Description: Deep dive into the cinematic pipeline for Mafia: The Old Country — how animation, motion capture, and real-time rendering were unified to eliminate jarring gameplay-to-cutscene transitions.
                  Registration required: No

                12:15–13:00 | PANEL — AI-Driven QA in Long-Running Projects
                  Panelists: Stefan Seicarescu, Nicusor Cojocaru, Mihai Racof
                  Moderator: Razvan Safta (Funcom)
                  Description: Practitioners share honest lessons from integrating AI into QA pipelines on live-service games, covering what scaled, what failed, and what they'd do differently.
                  Registration required: No

                13:00–14:00 | Lunch Break

                14:00–14:45 | KEYNOTE — Creativity in the Age of AI: Practicing Creative Sobriety
                  Speaker: Fawzi Mesmar
                  Description: A framework for maintaining creative authenticity and originality when AI tools are embedded in every step of the development process.
                  Registration required: No

                14:45–15:15 | The Division 2 – 7 Years Later – Evolving the Endgame
                  Speaker: Dragos Liche (Ubisoft)
                  Description: Development insights from shipping Retaliation, a major endgame feature for a seven-year-old live-service title, and what sustaining long-running games actually requires.
                  Registration required: No

                15:15–15:45 | Coffee Break

                15:45–16:15 | Crawl, Walk & Run: Forget Analytics as a Culture For Now
                  Speaker: Mathieu Ruiz (Funcom)
                  Description: A pragmatic guide for studios at different maturity levels on how to start using data and analytics without drowning in tooling or process overhead.
                  Registration required: No

                16:15–17:15 | PANEL — Game Production in 2026: Building Games Under Pressure
                  Panelists: Lisa Kretschmer (IO Interactive), Ionut Codreanu (Funcom), Mihai Sfrijan (Amber)
                  Moderator: Mehdi Benkirane (Zenith Pirates)
                  Description: Senior producers discuss modern production workflows, remote and hybrid team structures, and the efficiency strategies AA/AAA studios are using to ship under tighter constraints.
                  Registration required: No

                17:15–17:30 | Closing Ceremony
                  Speakers: Andreea Sava & Catalin Butnariu (RGDA)
                  Description: Official conference close and acknowledgements.
                  Registration required: No

                WORKSHOP STAGE

                10:30–11:00 | Workshop: Who Are You Building For? — Persona-Based Testing
                  Speaker: Radu Posoi (Alkotech Labs)
                  Description: Practical workshop on using player personas to structure playtesting sessions and surface the insights that matter to specific audience segments.
                  Registration required: Yes (registration form available on the website)

                12:00–13:00 | Tech Workshop: The Future of Software Architecture in the Age of AI
                  Speakers: Razvan Balasa (Playtika) & Adrian Coman (Crowdstrike)
                  Description: Hands-on session on LLM integration patterns, cost reduction strategies, and AI-infused application design for modern software teams.
                  Registration required: Yes (registration form available on the website)

                14:45–15:15 | Student Workshop: AI in Game Development — Magic Wand or Poisoned Gift?
                  Speaker: Alex Padure (Assist Software)
                  Description: Critical analysis of AI's practical role in game creation, covering ownership, liability, and the real creative impact on developers and players.
                  Registration required: Yes (registration form available on the website)

                16:00–17:00 | Roundtable: Under-funded. Under-represented. Underdogs.
                  Description: Private roundtable on strategies for regional studios competing in a market dominated by larger players.
                  Registration required: Private (invite only)
                """;
    }
}
