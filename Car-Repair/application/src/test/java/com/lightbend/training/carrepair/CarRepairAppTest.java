package com.lightbend.training.carrepair;


import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import org.assertj.core.data.*;

public class CarRepairAppTest {

  @Test
  public void argsToOptsShouldConvertArgsToOpts() {
    final Map<String, String> result = CarRepairApp.argsToOpts(Arrays.asList("a=1", "b", "-Dc=2"));
    assertThat(result).contains(MapEntry.entry("a", "1"), MapEntry.entry("-Dc", "2"));
  }


  @Test
  public void applySystemPropertiesShouldConvertOptsToSystemProps() {
    System.setProperty("c", "");
    Map<String, String> opts = new HashMap<>();
    opts.put("a", "1");
    opts.put("-Dc", "2");
    CarRepairApp.applySystemProperties(opts);
    assertThat(System.getProperty("c")).isEqualTo("2");
  }
}
