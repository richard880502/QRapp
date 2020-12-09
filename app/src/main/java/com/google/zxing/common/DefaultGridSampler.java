/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.common;

import android.util.Log;

import com.google.zxing.NotFoundException;

import java.util.Arrays;

/**
 * @author Sean Owen
 */
public final class DefaultGridSampler extends GridSampler {

  @Override
  public BitMatrix sampleGrid(BitMatrix image,
                              int dimensionX,
                              int dimensionY,
                              float p1ToX, float p1ToY,
                              float p2ToX, float p2ToY,
                              float p3ToX, float p3ToY,
                              float p4ToX, float p4ToY,
                              float p1FromX, float p1FromY,
                              float p2FromX, float p2FromY,
                              float p3FromX, float p3FromY,
                              float p4FromX, float p4FromY) throws NotFoundException {

    PerspectiveTransform transform = PerspectiveTransform.quadrilateralToQuadrilateral(
        p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY,
        p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY);

    return sampleGrid(image, dimensionX, dimensionY, transform);
  }

  @Override
  public BitMatrix sampleGrid(BitMatrix image,
                              int dimensionX,
                              int dimensionY,
                              PerspectiveTransform transform) throws NotFoundException {
    if (dimensionX <= 0 || dimensionY <= 0) {
      throw NotFoundException.getNotFoundInstance();
    }
    BitMatrix bits = new BitMatrix(dimensionX, dimensionY);
    //test
    float[] points = new float[2 * dimensionX];
    for (int y = 0; y < dimensionY; y++) {
      int max = points.length;
      float iValue = y + 0.5f;
      for (int x = 0; x < max; x += 2) {
        points[x] = (float) (x / 2) + 0.5f;
        points[x + 1] = iValue;
      }
      transform.transformPoints(points);
      // Quick check to see if points transformed to something inside the image;
      // sufficient to check the endpoints
      checkAndNudgePoints(image, points);
      try {
        for (int x = 0; x < max; x += 2) {
          if (image.get((int) points[x], (int) points[x + 1])) {
            // Black(-ish) pixel
            bits.set(x / 2, y);
          }
        }
      } catch (ArrayIndexOutOfBoundsException aioobe) {
        // This feels wrong, but, sometimes if the finder patterns are misidentified, the resulting
        // transform gets "twisted" such that it maps a straight line of points to a set of points
        // whose endpoints are in bounds, but others are not. There is probably some mathematical
        // way to detect this about the transformation that I don't know yet.
        // This results in an ugly runtime exception despite our clever checks above -- can't have
        // that. We could check each point's coordinates but that feels duplicative. We settle for
        // catching and wrapping ArrayIndexOutOfBoundsException.
        throw NotFoundException.getNotFoundInstance();
      }
    }

    //modified
    bits.setValue(find_state(image,dimensionX,dimensionY,bits,transform));
    //
    return bits;
  }

  //modified
  //
  //
  public int find_state(BitMatrix image,int dimensionX,
                        int dimensionY,
                        BitMatrix normal_bits,
                        PerspectiveTransform transform){

    float[] offsetX = {0.1f,0.9f,0.5f,0.5f};
    float[] offsetY = {0.5f,0.5f,0.9f,0.1f};
    int[] check = new int[4];

    for (int z = 0;z <check.length;z++) {
      BitMatrix bits = new BitMatrix(dimensionX, dimensionY);
      float[] points = new float[2 * dimensionX];
      for (int y = 0; y < dimensionY; y++) {
        int max = points.length;
        float iValue = y + offsetY[z];
        for (int x = 0; x < max; x += 2) {
          points[x] = (float) (x / 2) + offsetX[z];
          points[x + 1] = iValue;
        }
        transform.transformPoints(points);

        for (int x = 0; x < max; x += 2) {
          if (image.get((int) points[x], (int) points[x + 1])) {
            // Black(-ish) pixel
            bits.set(x / 2, y);
          }
        }
      }

      check[z] =0;
      for (int x = 0; x < bits.getHeight(); x++) {
        for (int y = 0; y < bits.getWidth(); y++) {
          //System.out.println(normal_bits.get(x,y));
          if (bits.get(x, y) != normal_bits.get(x, y))
            check[z]++;
        }
      }
    }
    int max = check[0],index = 1;
    for (int i =0;i < check.length;i++)
      if (check[i] >max) {
        max = check[i];
        index = i+1;
      }
    //System.out.println("check"+check[index]+" "+ index);
    return index;
  }
}
