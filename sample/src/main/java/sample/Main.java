package sample;

import java.util.stream.Stream;

public class Main {
  public static void main(String[] args) {
    System.out.println("** Resource types **");
    Stream.of(ResourceType.values()).map(rt -> ResourceTypeCompanion.toString(rt)).forEach(System.out::println);

    System.out.println("** Source types **");
    Stream.of(SourceType.values()).map(rt -> SourceTypeCompanion.toString(rt)).forEach(System.out::println);
  }
}
