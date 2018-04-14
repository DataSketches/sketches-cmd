/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLineParser; //interface
import org.apache.commons.cli.DefaultParser;     //current recommended implementation
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A Command Line Parser for the basic sketch functions. This is intentionally a very simple parser
 * with limited functionality that can be used for small experiments and for demos.
 *
 * <p>Although the sketching library can be used on a single machine, the more typical use case is
 * on large, highly distributed system architectures where command line access may not be practical.
 *
 * <p><b>TO INSTALL</b></p>
 *
 * <p>Fork this repository to a directory of your choice.
 *
 * <p>Install Maven if you don't have it.
 *
 * <p>Do a <i>mvn clean package</i> and then move the
 * <i>sketches-cmd-x.y.z-...-with-shaded-core.jar</i> to the root of your install
 * directory.
 *
 * <p>At the root of the directory is a bash shell script file <i>ds</i>. Make this file
 * executable with <i>chmod 755 ds</i> or equivalent.
 *
 * <p>Run the tool <i>./ds man</i>, which should print out the manual.
 *
 * @param <T> Sketch Type
 */

public abstract class SketchCommandLineParser<T> {
  public static final String LS = System.getProperty("line.separator");
  static final String BOLD = "\033[1m"; //4 char
  static final String OFF = "\033[0m";  //4 char

  private boolean updateFlag;
  ArrayList<T> sketches;
  Options options;
  org.apache.commons.cli.CommandLine cl;

  SketchCommandLineParser() {
    sketches = new ArrayList<>();
    options = new Options();
    options.addOption(Option.builder("d")
        .longOpt("data-from-file")
        .desc("read data from FILE")
        .hasArg()
        .argName("FILE")
        .build());
    options.addOption(Option.builder("s")
        .longOpt("sketch-input-files")
        .desc("read sketches from FILES")
        .hasArgs() //unlimited
        .argName("FILES")
        .build());
    options.addOption(Option.builder("o")
        .longOpt("sketch-output-file")
        .desc("save sketch to FILE")
        .hasArg()
        .argName("FILE")
        .build());
    options.addOption(Option.builder("help")
        .desc("usage/help")
        .build());
    options.addOption(Option.builder("p")
        .desc("print sketch summary")
        .longOpt("print")
        .build());
  }

  /**
   * Entry point
   * @param args array of tokens
   */
  public static void main(final String[] args) {
    if ((args == null) || (args.length == 0) || (args[0].isEmpty())) {
          help();
          return;
    }
    final String token0 = args[0].toLowerCase();
    switch (token0) {
      case "freq":
        final FrequenciesCL fcl = new FrequenciesCL();
        fcl.runCommandLineUtil(args);
        break;
      case "hll":
        final HllCL hllcl = new HllCL();
        hllcl.runCommandLineUtil(args);
        break;
      case "quant":
          final QuantilesCL qcl = new QuantilesCL();
          qcl.runCommandLineUtil(args);
          break;
      case "rsamp":
        final ReservoirSamplingCL rscl = new ReservoirSamplingCL();
        rscl.runCommandLineUtil(args);
        break;
      case "theta":
        final ThetaCL tcl = new ThetaCL();
        tcl.runCommandLineUtil(args);
        break;
      case "vsamp":
        final VarOptSamplingCL vscl = new VarOptSamplingCL();
        vscl.runCommandLineUtil(args);
        break;
      case "help":
      case "-help":
        help();
        break;
      case "man":
        manual();
        break;
      default: {
          printlnErr("Unrecognized Sketch Type: " + token0);
          help();
      }
    }
  }

  private static void manual() {
    help();
    println("");
    new FrequenciesCL().showHelp();
    println("");
    new HllCL().showHelp();
    println("");
    new QuantilesCL().showHelp();
    println("");
    new ReservoirSamplingCL().showHelp();
    println("");
    new ThetaCL().showHelp();
    println("");
    new VarOptSamplingCL().showHelp();
  }

  protected void runCommandLineUtil(final String[] args) {
    updateFlag = false;
    final CommandLineParser parser = new DefaultParser();
    try {
        cl = parser.parse(options, args);

        if (cl.hasOption("help")) {
          showHelp();
          return;
        }

        if (cl.hasOption("s")) {
          loadInputSketches();
          updateFlag = true;
        }

        if (sketches.size() > 1) {
          mergeSketches();
        } else if (sketches.size() == 0) {
          buildSketch();
        }

        updateCurrentSketch();
        if (updateFlag) {
          queryCurrentSketch();
          if (cl.hasOption("p")) {
            printCurrentSketchSummary();
          }
          if (cl.hasOption("o")) {
            saveCurrentSketch();
          }
        } else {
          showHelp();
        }
    } catch (final ParseException e) {
            printlnErr("runCommandLineUtil Error: " + e.getMessage());
    }
}

  //USED BY SUB-CLASSES

  protected abstract void showHelp();

  protected abstract void buildSketch();

  protected abstract void updateSketch(BufferedReader br);

  protected abstract T deserializeSketch(byte[] bytes);

  protected abstract byte[] serializeSketch(T sketch);

  protected abstract void mergeSketches();

  protected abstract void queryCurrentSketch();

  //Used by sub-classes that require multi-argument lines from a file
  protected String[] queryFileReader(final String pathToFile) {
    final ArrayList<String> values = new ArrayList<>();
    String itemStr = "";
    final String[] valuesArray;
    try (BufferedReader in =
        new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile)))) {
      while ((itemStr = in.readLine()) != null) {
        if (itemStr.isEmpty()) { continue; }
        values.add(itemStr);
      }
      valuesArray = new String[values.size()];
      for (int i = 0; i < valuesArray.length; i++) {
          valuesArray[i] = values.get(i);
      }
    }
    catch (final IOException  e ) {
      printlnErr("File Read Error: Item: " + itemStr );
      throw new RuntimeException(e);
    }
    return valuesArray;
  }

  protected static void printlnErr(final String s) {
    System.err.println(s);
  }

  protected static void println(final String s) {
    System.out.println(s);
  }

  //RESTRICTED

  private void loadInputSketches() {
      try {
        final String[] inputSketchesPathes = cl.getOptionValues("s");
        for (int i = 0; i < inputSketchesPathes.length; i++) {
          try (FileInputStream in = new FileInputStream(inputSketchesPathes[i])) {
            final byte[] bytes = new byte[in.available()];
            in.read(bytes);
            sketches.add(deserializeSketch(bytes));
          }
        }
      } catch (final IOException e) {
        printlnErr("loadInputSketches Error: " + e.getMessage());
      }
  }

  private void saveCurrentSketch() {
      final String fname = cl.getOptionValue("o");
      final File file = new File(fname);
      if (file.exists()) { file.delete(); }
      try (FileOutputStream out = new FileOutputStream(cl.getOptionValue("o"))) {
        out.write(serializeSketch(sketches.get(sketches.size() - 1)));
      } catch (final IOException e) {
        printlnErr("saveCurrentSketch Error: " + e.getMessage());
      }
  }

  private void updateCurrentSketch() {
    try {
      if (cl.hasOption("d")) { //data input from FILE
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(cl.getOptionValue("d")), UTF_8))) {
          updateSketch(br);
          updateFlag = true;
        }
      } else if (!cl.hasOption("s")) { //and NOT sketches from FILES
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(
            System.in, UTF_8))) {
          updateSketch(br);
          updateFlag = true;
        }
      }
    } catch (final IOException e) {
      printlnErr("updateCurrentSketch Error: " + e.getMessage());
    }
  }

  private void printCurrentSketchSummary() {
    final T sketch = sketches.get(sketches.size() - 1);
    println(LS + sketch.toString());
  }

  /**
   * Help function for level 0 tokens
   */
  private static void help() {
    final String spaces = "                  ";
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "DataSketches Command Line Manual" + OFF).append(LS + LS);
    sb.append(BOLD + "NAME" + OFF).append(LS);
    sb.append("  ds              ");
    sb.append("The DataSketches command-line application provides sketches for uniques,")
        .append(LS);
    sb.append(spaces + "distributions (quantiles, ranks, cdf, pdf), frequent items,").append(LS);
    sb.append(spaces + "and sampling of uniform and weighted items.").append(LS);
    sb.append(spaces + "For more information refer to https://datasketches.github.io.")
        .append(LS + LS);

    sb.append(BOLD + "SYNOPSIS" + OFF).append(LS);
    sb.append("  ds              ");
    sb.append("This help text.").append(LS + LS);
    sb.append("  ds help         ");
    sb.append("This help text.").append(LS + LS);
    sb.append("  ds SKETCH <OPT> ");
    sb.append("Use this SKETCH with options. ").append(LS + LS);
    sb.append("  ds SKETCH -help ");
    sb.append("Get options help for this SKETCH").append(LS + LS);
    sb.append("  ds man          ");
    sb.append("Print the entire manual").append(LS + LS);

    sb.append(BOLD + "SKETCH DESCRIPTIONS" + OFF).append(LS);

    sb.append("  ds freq         ");
    sb.append("Frequency sketch for finding the heavy hitter objects from a stream of").append(LS);
    sb.append(spaces + "integer weighted items. This sketch accumulates the weights keyed on "
        + "the items.").append(LS);
    sb.append(spaces + "The default file format is each line is a single item with an assumed "
        + "weight of 1.").append(LS);
    sb.append(spaces + "Output with no options is all No False Positives with estimate frequencies.")
        .append(LS + LS);

    sb.append("  ds hll          ");
    sb.append("HyperLogLog (HLL) sketch for estimating cardinalities from a stream of items.")
        .append(LS + LS);

    sb.append("  ds quant        ");
    sb.append("Quantiles sketch for estimating distributions from a stream of numeric values.")
        .append(LS);
    sb.append(spaces + "Output with no options is deciles.")
        .append(LS + LS);

    sb.append("  ds rsamp        ");
    sb.append("Reservior sketch for uniform sampling of a stream of items into max k samples.")
        .append(LS + LS);

    sb.append("  ds theta        ");
    sb.append("Theta sketch for estimating set expression cardinalities of a stream of items.")
        .append(LS + LS);

    sb.append("  ds vsamp        ");
    sb.append("Varopt sketch for weighted sampling of a stream of pre-aggregated, weighted items")
        .append(LS);
    sb.append(spaces + "into max k samples. This sketch does not accumulate weights of "
        + "identical items.").append(LS);
    sb.append(spaces + "The default file format is each line is a single item with an assumed "
        + "weight of 1.0.");

    println(sb.toString());
  }

}
