package ru.igojig.common.callback;

@FunctionalInterface
public interface ProgressBarActive {
    void progress(double received, double fullLength);
}
