package com.lightbend.training.carrepair;

import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

public class TerminalTest {

  @Test
  public void shouldCreateCorrectGuestFromCommand() {
    assertThat(Terminal.create("guest")).isEqualTo(new TerminalCommand.Guest(1, new Repair.Engine(), Integer.MAX_VALUE));
    assertThat(Terminal.create("2 g")).isEqualTo(new TerminalCommand.Guest(2, new Repair.Engine(), Integer.MAX_VALUE));
    assertThat(Terminal.create("g w")).isEqualTo(new TerminalCommand.Guest(1, new Repair.Wheels(), Integer.MAX_VALUE));
    assertThat(Terminal.create("g 1")).isEqualTo(new TerminalCommand.Guest(1, new Repair.Engine(), 1));
    assertThat(Terminal.create("2 g w 1")).isEqualTo(new TerminalCommand.Guest(2, new Repair.Wheels(), 1));
  }

  @Test
  public void shouldCreateGetStatusFromCommand() {
    assertThat(Terminal.create("status")).isEqualTo(TerminalCommand.Status.Instance);
    assertThat(Terminal.create("s")).isEqualTo(TerminalCommand.Status.Instance);

  }

  @Test
  public void shouldCreateQuitFromCommand() {
    assertThat(Terminal.create("quit")).isEqualTo(TerminalCommand.Quit.Instance);
    assertThat(Terminal.create("q")).isEqualTo(TerminalCommand.Quit.Instance);
  }

  @Test
  public void shouldCreateUnknownFromCommand() {
    assertThat(Terminal.create("foo")).isEqualTo(new TerminalCommand.Unknown("foo"));
  }
}
