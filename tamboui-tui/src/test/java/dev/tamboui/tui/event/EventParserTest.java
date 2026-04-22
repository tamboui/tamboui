package dev.tamboui.tui.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.bindings.Bindings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventParserTest {

  private Backend backend;
  private Bindings bindings;

  @BeforeEach
  void setUp() {
    backend = mock(Backend.class);
    bindings = mock(Bindings.class);
  }

  @Test
  void returnNullOnTimeout() throws Exception {
    when(backend.read(anyInt())).thenReturn(-2);

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertNull(event);
  }

  @Test
  void returnsNullOnEOF() throws Exception {
    when(backend.read(anyInt())).thenReturn(-1);

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertNull(event);
  }

  @Test
  void parsesPrintableCharacter() throws Exception {
    when(backend.read(anyInt())).thenReturn((int) 'a');

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertEquals('a', keyEvent.character());
  }

  @Test
  void doesNotParseNonPrintableCharacter() throws Exception {
    when(backend.read(anyInt())).thenReturn((int) '\u2060');

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertEquals(KeyCode.UNKNOWN, keyEvent.code());
  }

  @Test
  void parsesEnter() throws Exception {
    when(backend.read(anyInt())).thenReturn(13); // CR

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertEquals(KeyCode.ENTER, keyEvent.code());
  }

  @Test
  void parsesCtrlC() throws Exception {
    when(backend.read(anyInt())).thenReturn(3);

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertTrue(keyEvent.isCtrlC());
  }

  @Test
  void parsesStandaloneEscape() throws Exception {
    when(backend.read(anyInt())).thenReturn(27);     // ESC
    when(backend.peek(anyInt())).thenReturn(-2);      // nothing after ESC → standalone

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertEquals(KeyCode.ESCAPE, keyEvent.code());
  }

  @Test
  void parsesArrowUp() throws Exception {
    // ESC [ A
    when(backend.read(anyInt()))
            .thenReturn(27)              // ESC
            .thenReturn((int) '[')       // consume '['
            .thenReturn((int) 'A');      // arrow up
    when(backend.peek(anyInt()))
            .thenReturn((int) '[');      // peek po ESC

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertEquals(KeyCode.UP, keyEvent.code());
  }

  @Test
  void parsesAltKey() throws Exception {
    // ESC + 'x' → Alt+x
    when(backend.read(anyInt()))
            .thenReturn(27)              // ESC
            .thenReturn((int) 'x');      // consume 'x'
    when(backend.peek(anyInt()))
            .thenReturn((int) 'x');      // peek: printable char

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(KeyEvent.class, event);
    KeyEvent keyEvent = (KeyEvent) event;
    assertTrue(keyEvent.hasAlt());
    assertEquals('x', keyEvent.character());
  }

  @Test
  void parsesMouseLeftClick() throws Exception {
    // ESC [ < 0 ; 10 ; 5 M
    when(backend.read(anyInt()))
            .thenReturn(27)              // ESC
            .thenReturn((int) '[')       // consume '['
            .thenReturn((int) '<')       // SGR mouse prefix
            .thenReturn((int) '0')       // button code
            .thenReturn((int) ';')
            .thenReturn((int) '1').thenReturn((int) '0') // x=10
            .thenReturn((int) ';')
            .thenReturn((int) '5')       // y=5
            .thenReturn((int) 'M');      // press
    when(backend.peek(anyInt()))
            .thenReturn((int) '[');

    Event event = EventParser.readEvent(backend, 100, bindings);

    assertInstanceOf(MouseEvent.class, event);
    MouseEvent mouse = (MouseEvent) event;
    assertEquals(MouseEventKind.PRESS, mouse.kind());
    assertEquals(MouseButton.LEFT, mouse.button());
    assertEquals(9, mouse.x());   // 0-indexed
    assertEquals(4, mouse.y());
  }

}