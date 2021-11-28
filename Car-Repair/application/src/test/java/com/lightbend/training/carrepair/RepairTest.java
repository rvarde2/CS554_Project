package com.lightbend.training.carrepair;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;
import static com.lightbend.training.carrepair.Repair.*;

public class RepairTest {

  @Test
  public void repairShouldContains_EngineBodyAndWheels() {
    assertThat(Repair.REPAIRS).isEqualTo(ImmutableSet.of(
            new Engine(), new Body(), new Wheels()));
  }

  @Test
  public void orderShouldCreateCorrectRepairForGivenCode() {
    assertThat(order("E")).isEqualTo(new Engine());
    assertThat(order("e")).isEqualTo(new Engine());
    assertThat(order("B")).isEqualTo(new Body());
    assertThat(order("b")).isEqualTo(new Body());
    assertThat(order("W")).isEqualTo(new Wheels());
    assertThat(order("w")).isEqualTo(new Wheels());
  }

  @Test
  public void orderShouldThrowExceptionForWrongRepairCode() {
    try {
      order("1");
      fail("Should have raised exception for invalid order code");
    } catch(Exception e) {
      assertThat(e).isExactlyInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void otherRepairShouldReturnRepairDifferentFromGivenCode() {
    REPAIRS.forEach(c -> assertThat(orderOther(c)).isNotEqualTo(c));
  }
}
