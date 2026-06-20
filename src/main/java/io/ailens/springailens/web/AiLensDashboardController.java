package io.ailens.springailens.web;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.store.RingBufferEventStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai-lens")
public class AiLensDashboardController {

    private final RingBufferEventStore store;

    public AiLensDashboardController(RingBufferEventStore store) {
        this.store = store;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        List<AiCallEvent> events = store.getAll();

        long avgLatency = (long) events.stream()
                .mapToLong(AiCallEvent::latencyMs)
                .average()
                .orElse(0);

        int totalTokens = events.stream()
                .mapToInt(e -> e.promptTokens() + e.completionTokens())
                .sum();

        StringBuilder rows = new StringBuilder();
        List<AiCallEvent> reversed = events.reversed();
        for (AiCallEvent e : reversed) {
            rows.append("""
                <tr>
                    <td>%s</td>
                    <td class="prompt">%s</td>
                    <td class="response">%s</td>
                    <td>%d ms</td>
                    <td>%d / %d</td>
                    <td>%s</td>
                </tr>
            """.formatted(
                    e.model(),
                    truncate(e.prompt(), 80),
                    truncate(e.response(), 80),
                    e.latencyMs(),
                    e.promptTokens(),
                    e.completionTokens(),
                    e.timestamp().toString()
            ));
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="refresh" content="5">
                <title>spring-ai-lens</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body { font-family: -apple-system, sans-serif; background: #f5f5f5; color: #222; }
                    header { background: #1a1a2e; color: white; padding: 16px 32px; display: flex; align-items: center; gap: 12px; }
                    header h1 { font-size: 18px; font-weight: 600; }
                    header span { font-size: 12px; opacity: 0.6; }
                    .stats { display: flex; gap: 16px; padding: 24px 32px; }
                    .stat { background: white; border-radius: 8px; padding: 16px 24px; flex: 1; border: 1px solid #e0e0e0; }
                    .stat .label { font-size: 12px; color: #888; margin-bottom: 4px; }
                    .stat .value { font-size: 28px; font-weight: 600; }
                    .table-wrap { padding: 0 32px 32px; }
                    table { width: 100%%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; border: 1px solid #e0e0e0; }
                    th { background: #f9f9f9; text-align: left; padding: 12px 16px; font-size: 12px; color: #555; border-bottom: 1px solid #e0e0e0; }
                    td { padding: 12px 16px; font-size: 13px; border-bottom: 1px solid #f0f0f0; vertical-align: top; }
                    tr:last-child td { border-bottom: none; }
                    tr:hover td { background: #fafafa; }
                    .prompt, .response { max-width: 300px; color: #444; }
                    .empty { text-align: center; padding: 48px; color: #aaa; }
                </style>
            </head>
            <body>
                <header>
                    <h1>spring-ai-lens</h1>
                    <span>auto-refreshes every 5s</span>
                </header>
                <div class="stats">
                    <div class="stat"><div class="label">Total calls</div><div class="value">%d</div></div>
                    <div class="stat"><div class="label">Avg latency</div><div class="value">%d ms</div></div>
                    <div class="stat"><div class="label">Total tokens</div><div class="value">%d</div></div>
                </div>
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>Model</th>
                                <th>Prompt</th>
                                <th>Response</th>
                                <th>Latency</th>
                                <th>Tokens (in/out)</th>
                                <th>Timestamp</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    %s
                </div>
            </body>
            </html>
        """.formatted(
                events.size(),
                avgLatency,
                totalTokens,
                rows.toString(),
                events.isEmpty() ? "<p class=\"empty\">No LLM calls captured yet.</p>" : ""
        );
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }
}