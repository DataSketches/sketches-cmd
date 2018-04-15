/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class CommandLineTest {
  public static final String LS = System.getProperty("line.separator");
  String dataFileName1 = "data1.txt";
  String dataFileName2 = "data2.txt";
  String ranksFileName = "ranks.txt";
  String valuesFileName = "values.txt";
  String freqDataFileName = "freqData.txt";
  String freqQueryFileName = "freqQuery.txt";
  String serFileName1 = "ser1.bin";
  String serFileName2 = "ser2.bin";

  @AfterClass
  public void deleteFiles() {
    deleteFile(dataFileName1);
    deleteFile(dataFileName2);
    deleteFile(ranksFileName);
    deleteFile(valuesFileName);
    deleteFile(freqDataFileName);
    deleteFile(freqQueryFileName);
    deleteFile(serFileName1);
    deleteFile(serFileName2);
  }

  @Test
  public void checkAllHelp() {
    println("");
    SketchCommandLineParser.main(null);
    println("");
    SketchCommandLineParser.main(new String[] {""});
    println("");
    SketchCommandLineParser.main(new String[] {"freq", "-help"});
    println("");
    SketchCommandLineParser.main(new String[] {"hll", "-help"});
    println("");
    SketchCommandLineParser.main(new String[] {"quant", "-help"});
    println("");
    SketchCommandLineParser.main(new String[] {"rsamp", "-help"});
    println("");
    SketchCommandLineParser.main(new String[] {"theta", "-help"});
    println("");
    SketchCommandLineParser.main(new String[] {"vsamp", "-help"});
    println("");
    SketchCommandLineParser.main(new String[] { "-help" });
    println("");
    SketchCommandLineParser.main(new String[] { "help" });
    println("");
    print("INTENTIONAL ERROR: ");
    SketchCommandLineParser.main(new String[] { "abc" });
    println("");
  }

  @Test
  public void checkManual() {
    println("");
    SketchCommandLineParser.main(new String[] {"man"});
    println("");
  }

  //TEST UNIQUES
  @Test
  public void checkTheta() {
    println("\nCHECK THETA");
    println("Creating Data Files...");
    deleteFile(serFileName1);
    deleteFile(serFileName2);

    createUniquesFile(1, 20000, dataFileName1);
    createUniquesFile(15001, 20000, dataFileName2);

    println("\nUpdating Theta Sketch 1:");
    callMain("theta -k 4096 -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Theta Sketch 2:");
    callMain("theta -k 4096 -d " + dataFileName2 + " -o " + serFileName2);

    println("\nMerge Theta Sketches 1 and 2:");
    callMain("theta -k 4096 -s " + serFileName1 + " " + serFileName2);

    println("\nIntersect Theta Sketches 1 and 2:");
    callMain("theta -k 4096 -i -s " + serFileName1 + " " + serFileName2);

    println("\nTheta Sketch 1 minus Theta Sketch 2 and Summarize:");
    callMain("theta -k 4096 -p -m -s " + serFileName1 + " " + serFileName2);
  }

  @Test
  public void checkHll() {
    println("\nCHECK HLL");
    println("Creating Data Files...");
    deleteFile(serFileName1);
    deleteFile(serFileName2);

    createUniquesFile(0, 20000, dataFileName1);
    createUniquesFile(15000, 20000, dataFileName2); //overlap is 500

    println("\nUpdating HLL Sketch 1, default lgK=12:");
    callMain("hll -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating HLL Sketch 1:");
    callMain("hll -lgk 12 -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating HLL Sketch 2:");
    callMain("hll -lgk 12 -d " + dataFileName2 + " -o " + serFileName2);

    println("\nMerge HLL Sketch 1 and 2 and Summarize:");
    callMain("hll -lgk 12 -p -s " + serFileName1 + " " + serFileName2);
  }

  //TEST QUANTILES
  @Test
  public void checkQuantiles() {
    println("\nCHECK QUANTILES");
    println("Creating Data Files...");
    deleteFile(serFileName1);
    deleteFile(serFileName2);

    createUniquesFile(0, 20000, dataFileName1);

    println("\nUpdating Quantiles Sketch 1, default deciles");
    callMain("quant -k 256 -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Quantiles Sketch 2, default deciles");
    callMain("quant -k 256 -d " + dataFileName1 + " -o " + serFileName2);

    println("\nMerge Quantiles Sketch 1 and 2, summarize, default deciles");
    callMain("quant -k 256 -p -s " + serFileName1 + " " + serFileName2);

    println("\nQuery Histograms from Sketch 1, lin & log histograms");
    callMain("quant -k 256 -s " + serFileName1 + " -b 30 -h -lh 1");

    println("\nQuery specific values to ranks from list from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -v 1 10000 20000");

    println("\nQuery specific ranks to values from list from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -r 0 .5 1");

    createRanksFile(ranksFileName);
    println("\nQuery specific ranks to values from file from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -R " + ranksFileName);

    createValuesFile(valuesFileName);
    println("\nQuery specific values to ranks from file from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -V " + valuesFileName);
  }

  //TEST FREQUENT ITEMS
  @Test
  public void checkFreqItems() {
    println("\nCHECK FREQUENT ITEMS");
    println("Creating Data Files...");
    deleteFile(serFileName1);
    deleteFile(serFileName2);

    createFreqDataFile(freqDataFileName);
    createFreqQueryFile(freqQueryFileName);

    println("\nUpdating freq Items Sketch 1, default output");
    callMain("freq -w -d " + freqDataFileName);

    println("\nUpdating Freq Items Sketch 1, print error, N, top ids, top ids + freq.");
    callMain("freq -k 256 -w -e -n -t -T -d " + freqDataFileName + " -o " + serFileName1);

    println("\nUpdating Freq Items Sketch 2, default output.");
    callMain("freq -k 256 -w -d " + freqDataFileName + " -o " + serFileName2);

    println("\nMerge Freq Items Sketches 1 & 2, NoFalseNeg, default output.");
    callMain("freq -k 256 -y -s " + serFileName1 + " " + serFileName2);

    println("\nQuery specific item frequencies from Sketch 1 from list");
    callMain("freq -k 256 -s " + serFileName1 + " -f 19976 19977 20000");

    println("\nQuery specific item frequencies from Sketch 1 from file");
    callMain("freq -k 256 -s " + serFileName1 + " -F " +  freqQueryFileName);
  }

  //TEST RESERVOIR Samples
  @Test
  public void checkReservior() {
    println("\nCHECK RESERVIOR");
    println("Creating Data Files...");
    deleteFile(serFileName1);
    deleteFile(serFileName2);

    createUniquesFile(0, 20000, dataFileName1);

    println("\nUpdating Reservior Sketch 1, default k=32");
    callMain("rsamp -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Reservior Sketch 1");
    callMain("rsamp -k 25  -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Reservior Sketch 2 with summary");
    callMain("rsamp -k 25 -p -d " + dataFileName1 + " -o " + serFileName2);

    println("\nMerge Reservior Sketch 1 and 2 with summary");
    callMain("rsamp -k 25 -p -s " + serFileName1 + " " + serFileName2);
  }

  //TEST VarOpt
  @Test
  public void checkVarOpt() {
    println("\nCHECK VAROPT");
    println("Creating Data Files...");
    deleteFile(serFileName1);
    deleteFile(serFileName2);

    createFreqDataFile(freqDataFileName);

    println("\nUpdating VarOpt Items Sketch 1: samples");
    callMain("vsamp  -d " + freqDataFileName);

    println("\nUpdating VarOpt Items Sketch 1: n, r, samples");
    callMain("vsamp -k 64 -w -n -r -d " + freqDataFileName + " -o " + serFileName1);

    println("\nUpdating VarOpt Items Sketch 2: samples");
    callMain("vsamp -k 64 -w -d " + freqDataFileName + " -o " + serFileName2);

    println("\nMerge VarOpt Sketch 1 and 2 with summary");
    callMain("vsamp -k 64 -p -s " + serFileName1 + " " + serFileName2);
  }

  private static void createUniquesFile(int start, int len, String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    try (PrintWriter out = getPrintWriter(file)) {
      for (int i = 0; i < len; i++) {
        out.print(Integer.toString(i + start) + LS);
      }
    }
  }

  private static void createRanksFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    try (PrintWriter out = getPrintWriter(file)) {
      out.print("0.0" + LS);
      out.print("0.5" + LS);
      out.print("1.0");
    }
  }

  private static void createValuesFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    try (PrintWriter out = getPrintWriter(file)) {
      out.print("0" + LS);
      out.print("10000" + LS);
      out.print("20000");
    }
  }

  private static void createFreqDataFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    try (PrintWriter out = getPrintWriter(file)) {
      for (int i = 1; i <= 19975; i++) {
        out.print("1\t" + i + LS);
      }
      for (int i = 19976; i <= 20000; i++) {
        out.print(i + "\t" + i + LS);
      }
    }
  }

  private static void createFreqQueryFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    try (PrintWriter out = getPrintWriter(file)) {
      for (int i = 19976; i <= 20000; i++) {
        out.print(i + LS);
      }
    }
  }

  private static void callMain(String s) {
    println("> " + s);
    String[] cl = s.split(" +");
    SketchCommandLineParser.main(cl);
  }

  private static void deleteFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { file.delete(); }
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    System.out.println(s);
  }

  static void print(String s) {
    System.out.print(s);
  }

  /**
   * Gets PrintWriter for creating a new file and or appending to an existing file.
   * @param file the file
   * @return PrintWriter
   */
  static PrintWriter getPrintWriter(File file) {
    if (!file.isFile()) { // does not exist
      try {
        file.createNewFile();
      } catch (Exception e) {
        throw new RuntimeException("Cannot create file: " + file.getName() + LS + e);
      }
    }
    try {
      PrintWriter out =
          new PrintWriter(
              new BufferedWriter(
                  new OutputStreamWriter(
                      new FileOutputStream(file, true))));
      return out;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
