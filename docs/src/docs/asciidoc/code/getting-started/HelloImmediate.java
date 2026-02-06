//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.text.Text;

public class HelloImmediate {
    public static void main(String[] args) throws Exception {
        try (var backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();

            try (var terminal = new Terminal<>(backend)) {
				terminal.draw(frame -> {
				    var paragraph = Paragraph.builder()
				        .text(Text.from("Hello, Immediate Mode!"))
				        .build();
				    frame.renderWidget(paragraph, frame.area());
				});
			}
            // Wait for user input
            Thread.sleep(2000);
        }
    }
}