package com.georgespear.strategy;

import java.util.Random;

public class RandomStrategy implements Strategy {

    @Override
    public int getIndex(int queueSize) {
        return new Random().nextInt(queueSize);
    }
}
