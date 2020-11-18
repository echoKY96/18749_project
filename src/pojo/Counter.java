package pojo;

public class Counter {
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static long number = 0;

    public synchronized void increase() {
        number++;
        System.out.println("Number of games played: " + number);
    }
}
