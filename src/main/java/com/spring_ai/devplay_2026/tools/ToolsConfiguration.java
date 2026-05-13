package com.spring_ai.devplay_2026.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ToolsConfiguration {
    @Tool(name = "DevPlayInformations", description = "General information about DevPlay 2026 event")
    public String getDevPlayInformations() {
        return """
                    Dev.Play Conference 2026
                    Join us for an exciting two-day event in downtown Bucharest, featuring:
                    Fresh & Focused Track: Dive into specialized content tracks, including the External Development Track, along with Indie Game Development, AAA Development, Mobile Games and Fundraising.
                    Bigger Networking, Better Connections: With up to 500 attendees, professional matchmaking tools, and targeted business opportunities, Dev.Play is your chance to meet studios, publishers, investors, and service providers from across the world.
                    Indie Festival & Expo: Discover some of the most creative indie games from the region in our on-site and live-streamed Indie Showcase, complete with dev interviews, exclusive gameplay, and a dedicated Steam sale page.
                    Exclusive Events: Don’t miss out on the official Dev.Play party and VIP side events
                    World-Class Speakers: Hear from top voices in the industry—studio founders, tech innovators, creative directors, and investors—ready to share insights and actionable advice.
                    Get ready to learn, share, and celebrate the game development community in Romania and beyond. Join us for Dev.Play 2026!
                """;
    }

}
