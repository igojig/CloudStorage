package ru.igojig.common.callback;

@FunctionalInterface
public interface CountDown {
    void countDown(double received, double fullLength);
}
