package com.aisc.algoviz.ast.engine.debug;

import com.aisc.algoviz.ast.engine.model.ObservationPoint;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maps source observation points to executable JDI locations after the target class is prepared. */
@Component
public class BreakpointMapper {

  public MappingResult map(ReferenceType referenceType, List<ObservationPoint> observationPoints) {
    Map<Location, MappedBreakpoint> mappings = new LinkedHashMap<>();
    List<String> warnings = new ArrayList<>();

    for (ObservationPoint point : observationPoints) {
      if (!referenceType.name().equals(point.className())) {
        warnings.add(
            "Prepared class "
                + referenceType.name()
                + " does not match observation class "
                + point.className());
        continue;
      }

      try {
        Location location = firstExecutableLocation(referenceType, point);
        if (location == null) {
          warnings.add(
              "No executable location exists in source range "
                  + point.startLine()
                  + "-"
                  + point.endLine());
          continue;
        }
        if (mappings.containsKey(location)) {
          warnings.add(
              "Multiple observation points resolve to the same JDI location; keeping the first "
                  + "source point at line "
                  + point.lineNumber());
          continue;
        }
        mappings.put(location, new MappedBreakpoint(point, location));
      } catch (AbsentInformationException ex) {
        warnings.add(
            "Debug line information is unavailable for source range "
                + point.startLine()
                + "-"
                + point.endLine());
      }
    }

    return new MappingResult(List.copyOf(mappings.values()), List.copyOf(warnings));
  }

  private Location firstExecutableLocation(ReferenceType referenceType, ObservationPoint point)
      throws AbsentInformationException {
    for (int line = point.startLine(); line <= point.endLine(); line++) {
      List<Location> locations = referenceType.locationsOfLine(line);
      if (!locations.isEmpty()) {
        // Source columns are not available from a line-only JDI lookup. Keep the first location
        // for this source range and deduplicate it against other AST points below.
        return locations.get(0);
      }
    }
    return null;
  }

  public record MappedBreakpoint(ObservationPoint observationPoint, Location location) {}

  public record MappingResult(List<MappedBreakpoint> mappings, List<String> warnings) {}
}
