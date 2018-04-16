package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.TAB;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.frequencies.ErrorType;
import com.yahoo.sketches.frequencies.ItemsSketch;


  public class FrequenciesCL extends SketchCommandLineParser<ItemsSketch<String>> {

    private static final int DEFAULT_SIZE = 1024;

    FrequenciesCL() {
      super();
      // input options
      options.addOption(Option.builder("k")
          .desc("parameter k")
          .hasArg()
          .build());

      // output options
      options.addOption(Option.builder("t")
          .longOpt("topk-ids")
          .desc("query just identities for most frequent items")
          .build());
      options.addOption(Option.builder("T")
          .longOpt("topk-ids-with-freq")
          .desc("query identities & frequencies for most frequent items")
          .build());
      options.addOption(Option.builder("e")
          .longOpt("error-offset")
          .desc("query maximum error offset")
          .build());
      options.addOption(Option.builder("n")
          .longOpt("stream-length")
          .desc("query stream length")
          .build());
      options.addOption(Option.builder("y")
          .longOpt("no-false-negatives")
          .desc("use NO_FALSE_NEGATIVES")
          .build());
      options.addOption(Option.builder("f")
          .longOpt("id2freq")
          .desc("query frequencies for items with ids from list")
          .hasArgs() //unlimited
          .argName("ID")
          .build());
      options.addOption(Option.builder("F")
          .longOpt("id2freq-file")
          .desc("query frequencies for items with ids from FILE")
          .hasArg()
          .argName("FILE")
          .build());
      options.addOption(Option.builder("w")
          .desc("Each line is two tokens separated by a tab, comma, or spaces. "
              + "Token 0 is an integer weight, the second token is the item. "
              + "If there is only one token it is assumed to be the item with weight = 1.")
          .longOpt("weights")
          .build());
    }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp("ds freq", options);
  }

  protected ItemsSketch<String> buildSketch() {
    final int k = cl.hasOption("k") ? Integer.parseInt(cl.getOptionValue("k")) : DEFAULT_SIZE;
    return new ItemsSketch<>(k);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    final ItemsSketch<String> sketch = buildSketch();
    String itemStr = "";
    try {
      if (cl.hasOption("w")) {
        while ((itemStr = br.readLine()) != null) {
          if (itemStr.isEmpty()) { continue; }
          final String[] tokens = itemStr.split("[\\t, ]+", 2);
          if (tokens.length < 2) {
            sketch.update(tokens[1], 1);
          } else {
            sketch.update(tokens[1], Long.parseLong(tokens[0]));
          }
        }
      } else { //assume entire line is item
        while ((itemStr = br.readLine()) != null) {
          if (itemStr.isEmpty()) { continue; }
          sketch.update(itemStr, 1);
        }
      }
      sketchList.add(sketch);
    } catch (final IOException | NumberFormatException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ItemsSketch<String> deserializeSketch(final byte[] bytes) {
    return ItemsSketch.getInstance(Memory.wrap(bytes), new ArrayOfStringsSerDe());
  }

  @Override
  protected byte[] serializeSketch(final ItemsSketch<String> sketch) {
    return sketch.toByteArray(new ArrayOfStringsSerDe());
  }

  @Override
  protected void mergeSketches() {
    final ItemsSketch<String> union;
    if (cl.hasOption("k")) { //user defined k
      union = new ItemsSketch<>(Integer.parseInt(cl.getOptionValue("k")));
    } else { //default k
      union = new ItemsSketch<>(DEFAULT_SIZE);
    }
    for (final ItemsSketch<String> sketch: sketchList) {
      union.merge(sketch);
    }
    sketchList.add(union);
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketchList.size() > 0) {
      final ItemsSketch<String> sketch = sketchList.get(sketchList.size() - 1);
      boolean optionChosen = false;
      ErrorType errType = ErrorType.NO_FALSE_POSITIVES;

      if (cl.hasOption("y")) {
        errType = ErrorType.NO_FALSE_NEGATIVES;
      }

      if (cl.hasOption("e")) {
        final String errOff = Long.toString(sketch.getMaximumError());
        println("Max Error Offset: " + errOff);
      }

      if (cl.hasOption("n")) {
        final String n = Long.toString(sketch.getStreamLength());
        println("Stream Length   : " + n);
      }

      if (cl.hasOption("t")) { //print only items of ErrorType
        optionChosen = true;
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(errType);
        println("ErrorType = " + errType.toString());
        println("\nItems");
        for (int i = 0; i < rowArr.length; i++) {
          println(rowArr[i].getItem());
        }
      }

      if (cl.hasOption("T")) { //print item & freq of ErrorType
        optionChosen = true;
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(errType);
        println("ErrorType = " + errType.toString());
        println("\nItems" + TAB + "Frequency");
        for (int i = 0; i < rowArr.length; i++) {
          println(rowArr[i].getItem() + TAB + rowArr[i].getEstimate());
        }
      }

      if (cl.hasOption("f")) { //print items and freq from list
        optionChosen = true;
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(errType);
        final String[] items = cl.getOptionValues("f");
        println("\nItems" + TAB + "Frequency");
        for (int i = 0; i < items.length; i++) {
          long freq = 0;
          for (int j = 0; j < rowArr.length; j++) {
            if (rowArr[j].getItem().equals(items[i])) {
              freq = rowArr[j].getEstimate();
            }
          }
          println(items[i] + TAB + freq);
        }
      }

      if (cl.hasOption("F")) { //print items and freq from file
        optionChosen = true;
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(errType);
        final String[] items = queryFileReader(cl.getOptionValue("F"));
        println("\nItems" + TAB + "Frequency");
        for (int i = 0; i < items.length; i++) {
          long freq = 0;
          for (int j = 0; j < rowArr.length; j++) {
            if (rowArr[j].getItem().equals(items[i])) {
              freq = rowArr[j].getEstimate();
            }
          }
          println(items[i] + TAB + freq);
        }
      }

      //Default: print item & freq of ErrorType, same as opt T
      if (!optionChosen) {
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(errType);
        println("ErrorType = " + errType.toString());
        println("\nItems" + TAB + "Frequency");
        for (int i = 0; i < rowArr.length; i++) {
          println(rowArr[i].getItem() + TAB + rowArr[i].getEstimate());
        }
      }
    }
  }

}
