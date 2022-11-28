package ru.igojig.common.callback;

@FunctionalInterface
public interface ProgressBarAction {
    void progress(double partLength, double fullLength);
}
