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
   protected UpdateSketch updateSketch;

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
          .desc("from the first sketch subtract all others")
          .build());
   }

  @Override
  protected void showHelp() {
    final HelpFormatter helpf = new HelpFormatter();
    helpf.setOptionComparator(null);
    helpf.printHelp("ds theta", options);
  }


  @Override
  protected void buildSketch() {
    final UpdateSketchBuilder bldr = Sketches.updateSketchBuilder();
    if (cl.hasOption("k")) {
      bldr.setNominalEntries(Integer.parseInt(cl.getOptionValue("k")));  // user defined k
    }
    updateSketch = bldr.build();
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    if (sketchList.size() > 0) {
      buildSketch();
    }
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        updateSketch.update(itemStr);
      }
    } catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    if (sketchList.size() > 0) {
      final Union union = SetOperation.builder().buildUnion();
      union.update(sketchList.get(sketchList.size() - 1));
      union.update(updateSketch);
      sketchList.add(union.getResult());
    } else {
      sketchList.add(updateSketch.compact());
    }
  }

  @Override
  protected Sketch deserializeSketch(final byte[] bytes) {
    return Sketch.wrap(Memory.wrap(bytes));
  }

  @Override
  protected byte[] serializeSketch(final Sketch sketch) {
    return sketch.toByteArray();
  }


  @Override
  protected void mergeSketches() {
      if (cl.hasOption("i")) {
        final Intersection intersection = SetOperation.builder().buildIntersection();
        for (Sketch sketch: sketchList) {
          intersection.update(sketch);
        }
        sketchList.add(intersection.getResult());
        return;
      }

      if (cl.hasOption("m")) {
        final Union union = SetOperation.builder().buildUnion();
        for (int i = 1; i < sketchList.size(); i++) { //skip the first one
          union.update(sketchList.get(i));
        }

        final AnotB aNotB = Sketches.setOperationBuilder().buildANotB();
        aNotB.update(sketchList.get(0), union.getResult());
        sketchList.add(aNotB.getResult());
        return;
      }

      // default merge is union
      final SetOperationBuilder builder = SetOperation.builder();
      if (cl.hasOption("k")) { // user defined k
        builder.setNominalEntries(Integer.parseInt(cl.getOptionValue("k")));
      }
      final Union union = builder.buildUnion();
      for (Sketch sketch: sketchList) {
        union.update(sketch);
      }
      sketchList.add(union.getResult());
      return;
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketchList.size() > 0) {
      final Sketch sketch = sketchList.get(sketchList.size() - 1);
      final double est = sketch.getEstimate();
      final double lb = sketch.getLowerBound(2);
      final double ub = sketch.getUpperBound(2);
      System.out.format("%f %f %f\n",lb, est, ub);
    }
  }
}
