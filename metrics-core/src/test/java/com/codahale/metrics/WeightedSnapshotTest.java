package com.codahale.metrics;

import com.codahale.metrics.WeightedSnapshot.WeightedSample;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class WeightedSnapshotTest {

    private static List<WeightedSample> weightedArray(long[] values, double[] weights) {
        if (values.length != weights.length) {
            throw new IllegalArgumentException("Mismatched lengths: " + values.length + " vs " + weights.length);
        }

        final List<WeightedSample> samples = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            samples.add(new WeightedSnapshot.WeightedSample(values[i], weights[i]));
        }

        return samples;
    }

    private final WeightedSnapshot snapshot = new WeightedSnapshot(
            weightedArray(new long[]{5, 1, 2, 3, 4}, new double[]{1, 2, 3, 2, 2}));

    @Test
    public void smallQuantilesAreTheFirstValue() {
        assertThat(snapshot.getValue(0.0))
                .isEqualTo(1.0, offset(0.1));
    }

    @Test
    public void bigQuantilesAreTheLastValue() {
        assertThat(snapshot.getValue(1.0))
                .isEqualTo(5.0, offset(0.1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void disallowsNotANumberQuantile() {
        snapshot.getValue(Double.NaN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disallowsNegativeQuantile() {
        snapshot.getValue(-0.5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void disallowsQuantileOverOne() {
        snapshot.getValue(1.5);
    }

    @Test
    public void hasAMedian() {
        assertThat(snapshot.getMedian()).isEqualTo(3.0, offset(0.1));
    }

    @Test
    public void hasAp75() {
        assertThat(snapshot.get75thPercentile()).isEqualTo(4.0, offset(0.1));
    }

    @Test
    public void hasAp95() {
        assertThat(snapshot.get95thPercentile()).isEqualTo(5.0, offset(0.1));
    }

    @Test
    public void hasAp98() {
        assertThat(snapshot.get98thPercentile()).isEqualTo(5.0, offset(0.1));
    }

    @Test
    public void hasAp99() {
        assertThat(snapshot.get99thPercentile()).isEqualTo(5.0, offset(0.1));
    }

    @Test
    public void hasAp999() {
        assertThat(snapshot.get999thPercentile()).isEqualTo(5.0, offset(0.1));
    }

    @Test
    public void hasValues() {
        assertThat(snapshot.getValues())
                .containsOnly(1, 2, 3, 4, 5);
    }

    @Test
    public void hasASize() {
        assertThat(snapshot.size())
                .isEqualTo(5);
    }

    @Test
    public void worksWithUnderestimatedCollections() {
        final List<WeightedSample> originalItems = weightedArray(new long[]{5, 1, 2, 3, 4}, new double[]{1, 2, 3, 2, 2});
        final List<WeightedSample> spyItems = spy(originalItems);
        doReturn(originalItems.toArray(new WeightedSample[]{})).when(spyItems).toArray(ArgumentMatchers.any(WeightedSample[].class));
        when(spyItems.size()).thenReturn(4, 5);

        final Snapshot other = new WeightedSnapshot(spyItems);

        assertThat(other.getValues())
                .containsOnly(1, 2, 3, 4, 5);
    }

    @Test
    public void worksWithOverestimatedCollections() {
        final List<WeightedSample> originalItems = weightedArray(new long[]{5, 1, 2, 3, 4}, new double[]{1, 2, 3, 2, 2});
        final List<WeightedSample> spyItems = spy(originalItems);
        doReturn(originalItems.toArray(new WeightedSample[]{})).when(spyItems).toArray(ArgumentMatchers.any(WeightedSample[].class));
        when(spyItems.size()).thenReturn(6, 5);

        final Snapshot other = new WeightedSnapshot(spyItems);

        assertThat(other.getValues())
                .containsOnly(1, 2, 3, 4, 5);
    }

    @Test
    public void dumpsToAStream() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        snapshot.dump(output);

        assertThat(output.toString())
                .isEqualTo(String.format("1%n2%n3%n4%n5%n"));
    }

    @Test
    public void calculatesTheMinimumValue() {
        assertThat(snapshot.getMin())
                .isEqualTo(1);
    }

    @Test
    public void calculatesTheMaximumValue() {
        assertThat(snapshot.getMax())
                .isEqualTo(5);
    }

    @Test
    public void calculatesTheMeanValue() {
        assertThat(snapshot.getMean())
                .isEqualTo(2.7);
    }

    @Test
    public void calculatesTheStdDev() {
        assertThat(snapshot.getStdDev())
                .isEqualTo(1.2688, offset(0.0001));
    }

    @Test
    public void calculatesAMinOfZeroForAnEmptySnapshot() {
        final Snapshot emptySnapshot = new WeightedSnapshot(
                weightedArray(new long[]{}, new double[]{}));

        assertThat(emptySnapshot.getMin())
                .isZero();
    }

    @Test
    public void calculatesAMaxOfZeroForAnEmptySnapshot() {
        final Snapshot emptySnapshot = new WeightedSnapshot(
                weightedArray(new long[]{}, new double[]{}));

        assertThat(emptySnapshot.getMax())
                .isZero();
    }

    @Test
    public void calculatesAMeanOfZeroForAnEmptySnapshot() {
        final Snapshot emptySnapshot = new WeightedSnapshot(
                weightedArray(new long[]{}, new double[]{}));

        assertThat(emptySnapshot.getMean())
                .isZero();
    }

    @Test
    public void calculatesAStdDevOfZeroForAnEmptySnapshot() {
        final Snapshot emptySnapshot = new WeightedSnapshot(
                weightedArray(new long[]{}, new double[]{}));

        assertThat(emptySnapshot.getStdDev())
                .isZero();
    }

    @Test
    public void calculatesAStdDevOfZeroForASingletonSnapshot() {
        final Snapshot singleItemSnapshot = new WeightedSnapshot(
                weightedArray(new long[]{1}, new double[]{1.0}));

        assertThat(singleItemSnapshot.getStdDev())
                .isZero();
    }

    @Test
    public void expectNoOverflowForLowWeights() {
        final Snapshot scatteredSnapshot = new WeightedSnapshot(
                weightedArray(
                        new long[]{1, 2, 3},
                        new double[]{Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE}
                )
        );

        assertThat(scatteredSnapshot.getMean())
                .isEqualTo(2);
    }

    @Test
    public void doesNotProduceNaNValues() {
        WeightedSnapshot weightedSnapshot = new WeightedSnapshot(
                weightedArray(new long[]{1, 2, 3}, new double[]{0, 0, 0}));
        assertThat(weightedSnapshot.getMean()).isEqualTo(0);
    }

}
