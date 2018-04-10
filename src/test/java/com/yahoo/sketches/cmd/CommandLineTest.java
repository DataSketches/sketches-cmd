/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.cmd.Files.appendStringToFile;

import java.io.File;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class CommandLineTest {
  public static final String LS = System.getProperty("line.separator");


  @Test  //enable/disable here for visual checking
  public void outputHelp() {
    String[] line = new String[] {""};
    CommandLine.main(line);

    println("");
    line = new String[] {"freq", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"hll", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"quant", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"rsamp", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"theta", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"vsamp", "-help"};
    CommandLine.main(line);
  }

  //TEST UNIQUES
  @Test
  public void checkTheta() {
    String data1 = "data1.txt";
    String data2 = "data2.txt";
    String ser1 = "ser1.bin";
    String ser2 = "ser2.bin";

    deleteFile(data1);
    deleteFile(data2);
    deleteFile(ser1);
    deleteFile(ser2);

    createUniquesFile(1, 20000, data1);
    createUniquesFile(15001, 20000, data2);

    println("Sketch 1:");
    callMain("theta -k 4096 -d " + data1 + " -o " + ser1);

    println("Sketch 2:");
    callMain("theta -k 4096 -d " + data2 + " -o " + ser2);

    println("Merge:");
    callMain("theta -k 4096 -s " + ser1 + " " + ser2); //merge

    println("Intersect:");
    callMain("theta -k 4096 -i -s " + ser1 + " " + ser2); //intersect

    println("Difference:");
    callMain("theta -k 4096 -p -m -s " + ser1 + " " + ser2); //diff + summary
  }

  @Test
  public void checkHll() {
    String data1 = "data1.txt";
    String data2 = "data2.txt";
    String ser1 = "ser1.bin";
    String ser2 = "ser2.bin";

    deleteFile(data1);
    deleteFile(data2);
    deleteFile(ser1);
    deleteFile(ser2);

    createUniquesFile(1, 20000, data1);
    createUniquesFile(15001, 20000, data2);

    println("Sketch 1:");
    callMain("hll -lgk 12 -d " + data1 + " -o " + ser1);

    println("Sketch 2:");
    callMain("hll -lgk 12 -d " + data2 + " -o " + ser2);

    println("Merge:");
    callMain("hll -lgk 12 -p -s " + ser1 + " " + ser2); //merge + summary
  }

  private static void createUniquesFile(int start, int len, String fileName) {
    for (int i = 0; i < len; i++) {
      appendStringToFile(Integer.toString(i + start) + LS, fileName);
    }
  }

  //TEST QUANTILES




  private static void callMain(String s) {
    String[] cl = s.split(" +");
    CommandLine.main(cl);
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
    System.out.println(s); //disable here
  }
}
