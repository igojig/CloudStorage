package ru.igojig.common.callback;

@FunctionalInterface
public interface ProgressBarAction {
    void progress(double received, double fullLength);
}
