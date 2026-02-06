//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.widgets.gauge.Gauge;

public class HelloInline {
    public static void main(String[] args) throws Exception {
        try (var display = InlineDisplay.create(2)) {
            for (int i = 0; i <= 100; i += 5) {
                int progress = i;
                display.render((area, buffer) -> {
                    var gauge = Gauge.builder()
                        .ratio(progress / 100.0)
                        .label("Progress: " + progress + "%")
                        .build();
                    gauge.render(area, buffer);
                });
                Thread.sleep(50);
            }
            display.println("Done!");
        }
    }
}