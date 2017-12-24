import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import edu.hit.ir.ltp4j.SplitSentence;
import edu.hit.ir.ltp4j.Segmentor;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.NER;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.SRL;
import edu.hit.ir.ltp4j.Pair;

public class Console {
  private String segmentModel;
  private String postagModel;
  private String NERModel;
  private String parserModel;
  private String SRLModel;

  private SplitSentence sentenceSplitApp;
  private Segmentor     segmentorApp;
  private Postagger     postaggerApp;
  private NER           nerApp;
  private Parser        parserApp;
  private SRL           srlApp;

  private boolean ParseArguments(String[] args) {
    if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
      Usage();
      return false;
    }

    for (int i = 0; i < args.length; ++ i) {
      if (args[i].startsWith("--segment-model=")) {
        segmentModel = args[i].split("=")[1];
      } else if (args[i].startsWith("--postag-model=")) {
        postagModel = args[i].split("=")[1];
      } else if (args[i].startsWith("--ner-model=")) {
        NERModel = args[i].split("=")[1];
      } else if (args[i].startsWith("--parser-model=")) {
        parserModel = args[i].split("=")[1];
      } else if (args[i].startsWith("--srl-model=")) {
        SRLModel = args[i].split("=")[1];
      } else {
        throw new IllegalArgumentException("Unknown options " + args[i]);
      }
    }

    if (segmentModel == null || postagModel == null || NERModel == null ||
        parserModel == null || SRLModel == null) {
      Usage();
      throw new IllegalArgumentException("");
    }

    sentenceSplitApp = new SplitSentence();

    segmentorApp = new Segmentor();
    segmentorApp.create(segmentModel);

    postaggerApp = new Postagger();
    postaggerApp.create(postagModel);

    nerApp = new NER();
    nerApp.create(NERModel);

    parserApp = new Parser();
    parserApp.create(parserModel);

    srlApp = new SRL();
    srlApp.create(SRLModel);

    return true;
  }

  public void Usage() {
    System.err.println("An command line example for ltp4j - The Java embedding of LTP");
    System.err.println("Sentences are inputted from stdin.");
    System.err.println("");
    System.err.println("Usage:");
    System.err.println("");
    System.err.println("  java -cp <jar-path> --segment-model=<path> \\");
    System.err.println("                      --postag-model=<path> \\");
    System.err.println("                      --ner-model=<path> \\");
    System.err.println("                      --parser-model=<path> \\");
    System.err.println("                      --srl-model=<path>");
  }

  private String join(ArrayList<String> payload, String conjunction) {
    StringBuilder sb = new StringBuilder();
    if (payload == null || payload.size() == 0) {
      return "";
    }
    sb.append(payload.get(0));
    for (int i = 1; i < payload.size(); ++ i) {
      sb.append(conjunction).append(payload.get(i));
    }
    return sb.toString();
  }


  public void Analyse(String sent) {
    ArrayList<String> sents = new ArrayList<String>();
    sentenceSplitApp.splitSentence(sent, sents);

    for(int m = 0; m < sents.size(); m++) {
      ArrayList<String> words = new ArrayList<String>();
      ArrayList<String> postags = new ArrayList<String>();
      ArrayList<String> ners = new ArrayList<String>();
      ArrayList<Integer> heads = new ArrayList<Integer>();
      ArrayList<String> deprels = new ArrayList<String>();
      List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls =
        new ArrayList<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>>();

      System.out.println("#" + (m + 1));
      System.out.println("Sentence       : " + sents.get(m));

      segmentorApp.segment(sents.get(m), words);
      System.out.println("Segment Result : " + join(words, "\t"));

      postaggerApp.postag(words, postags);
      System.out.print("Postag Result  : ");
      System.out.println(join(postags, "\t"));

      nerApp.recognize(words, postags, ners);
      System.out.print("NER Result     : ");
      System.out.println(join(ners, "\t"));

      parserApp.parse(words, postags, heads, deprels);
      int size = heads.size();
      StringBuilder sb = new StringBuilder();
      sb.append(heads.get(0)).append(":").append(deprels.get(0));
      for(int i = 1; i < heads.size(); i++) {
        sb.append("\t").append(heads.get(i)).append(":").append(deprels.get(i));
      }
      System.out.print("Parse Result   : ");
      System.out.println(sb.toString());

      for (int i = 0; i < heads.size(); i++) {
        heads.set(i, heads.get(i) - 1);
      }

      srlApp.srl(words,postags,heads,deprels,srls);

      size = srls.size();
      System.out.print("SRL Result     : ");
      if (size == 0) {
        System.out.println();
      }
      for (int i = 0; i < srls.size(); i++) {
        System.out.print(srls.get(i).first + " ->");
        for (int j = 0; j < srls.get(i).second.size(); j++) {
          System.out.print(srls.get(i).second.get(j).first
              + ": beg = " + srls.get(i).second.get(j).second.first
              + " end = " + srls.get(i).second.get(j).second.second + ";");
        }
        System.out.println();
      }
    }
  }

  public void release(){
    segmentorApp.release();
    postaggerApp.release();
    nerApp.release();
    parserApp.release();
    srlApp.release();
  }

  public static void main(String[] args) {
    Console console = new Console();

    try {
      if (!console.ParseArguments(args)) {
        return;
      }

      Scanner input = new Scanner(System.in);
      String sent;
      try {
        System.out.print(">>> ");
        while((sent = input.nextLine()) != null) {
          if (sent.length() > 0) {
            console.Analyse(sent);
          }
          System.out.print(">>> ");
        }
      } catch(Exception e) {
        console.release();
      }
    } catch (IllegalArgumentException e) {
    }
  }
}
