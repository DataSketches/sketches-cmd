package com.yahoo.sketches.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.theta.AnotB;
import com.yahoo.sketches.theta.Intersection;
import com.yahoo.sketches.theta.SetOperation;
import com.yahoo.sketches.theta.SetOperationBuilder;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.Union;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

public class ThetaCL extends SketchCommandLineParser<Sketch> {

   ThetaCL() {
      super();
      // input options
      options.addOption(Option.builder("k")
          .desc("parameter k")
          .hasArg()
          .build());
      // sketch level operators
      options.addOption(Option.builder("i")
          .longOpt("intersection")
          .desc("intersection of sketches")
          .build());
      options.addOption(Option.builder("m")
          .longOpt("set-minus")
          .desc("AnotB: From the first sketch subtract all others. "
          + "If '-d' is specified, it becomes the 'A' sketch.")
          .build());
      options.addOption(Option.builder("b")
          .longOpt("bounds")
          .desc("output the 95% confidence bounds along with the estimate")
          .build());
   }

  @Override
  protected void showHelp() {
    final HelpFormatter helpf = new HelpFormatter();
    helpf.setOptionComparator(null);
    helpf.printHelp("ds theta", options);
  }


  protected UpdateSketch buildSketch() {
    final UpdateSketchBuilder bldr = Sketches.updateSketchBuilder();
    if (cl.hasOption("k")) {
      bldr.setNominalEntries(Integer.parseInt(cl.getOptionValue("k")));  // user defined k
    }
    return bldr.build();
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    final UpdateSketch updateSketch = buildSketch();
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        updateSketch.update(itemStr);
      }
      sketchList.add(updateSketch.compact());
    } catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Sketch deserializeSketch(final byte[] bytes) {
    return Sketch.wrap(Memory.wrap(bytes));
  }

  @Override
  protected byte[] serializeSketch(final Sketch sketch) {
    if (sketch instanceof UpdateSketch) {
      return ((UpdateSketch) sketch).compact().toByteArray();
    }
    return sketch.toByteArray();
  }


  @Override
  protected void mergeSketches() {
    //INTERSECTION
    if (cl.hasOption("i")) { //-i and -m are mutually exclusive
      final Intersection intersection = SetOperation.builder().buildIntersection();
      for (Sketch sketch: sketchList) { //intersect all sketches in list
        intersection.update(sketch);
      }
      sketchList.add(intersection.getResult()); //add result at the end of list
      return;
    }
    //A NOT B
    if (cl.hasOption("m")) {
      final Union union = SetOperation.builder().buildUnion();
      //union all sketches in list except the first one
      for (int i = 1; i < sketchList.size(); i++) { //skip the first one
        union.update(sketchList.get(i));
      }

      final AnotB aNotB = Sketches.setOperationBuilder().buildANotB();
      aNotB.update(sketchList.get(0), union.getResult()); //A = first one, B = Union
      sketchList.add(aNotB.getResult()); //add result at the end of list
      return;
    }

    // otherwise union
    final SetOperationBuilder builder = SetOperation.builder();
    if (cl.hasOption("k")) { // user defined k
      builder.setNominalEntries(Integer.parseInt(cl.getOptionValue("k")));
    }
    final Union union = builder.buildUnion();
    for (Sketch sketch: sketchList) {
      union.update(sketch);
    }
    sketchList.add(union.getResult()); //add result at the end
    return;
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketchList.size() > 0) {
      final Sketch sketch = sketchList.get(sketchList.size() - 1);
      final double est = sketch.getEstimate();
      final String s;

      if (cl.hasOption("b")) {
        final double lb = sketch.getLowerBound(2);
        final double ub = sketch.getUpperBound(2);
        s = String.format("%.0f   %.0f   %.0f", lb, est, ub);
      } else {
        s = String.format("%.0f", est);
      }
      println(s);
    }
  }
}
