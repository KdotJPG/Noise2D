/*
 *
 * MIT License
 *
 * Copyright (c) 2020 TerraForged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.dags.noise.util;

/**
 * Apache Commons Math
 */
class FastMath {

    private static final int EXP_INT_TABLE_MAX_INDEX = 750;
    private static final double LN_2_A = 0.693147063255310059;
    private static final double LN_2_B = 1.17304635250823482e-7;
    private static final long HEX_40000000 = 0x40000000L;
    private static final long MASK_DOUBLE_EXPONENT = 0x7ff0000000000000L;
    private static final long MASK_DOUBLE_MANTISSA = 0x000fffffffffffffL;
    private static final long IMPLICIT_HIGH_BIT = 0x0010000000000000L;
    private static final double TWO_POWER_52 = 4503599627370496.0;

    private FastMath() {}

    public static double pow(final double x, final double y) {
        if (y == 0) {
            // y = -0 or y = +0
            return 1.0;
        } else {

            final long yBits = Double.doubleToRawLongBits(y);
            final int yRawExp = (int) ((yBits & MASK_DOUBLE_EXPONENT) >> 52);
            final long yRawMantissa = yBits & MASK_DOUBLE_MANTISSA;
            final long xBits = Double.doubleToRawLongBits(x);
            final int xRawExp = (int) ((xBits & MASK_DOUBLE_EXPONENT) >> 52);
            final long xRawMantissa = xBits & MASK_DOUBLE_MANTISSA;

            if (yRawExp > 1085) {
                // y is either a very large integral value that does not fit in a long or it is a special number

                if ((yRawExp == 2047 && yRawMantissa != 0) ||
                        (xRawExp == 2047 && xRawMantissa != 0)) {
                    // NaN
                    return Double.NaN;
                } else if (xRawExp == 1023 && xRawMantissa == 0) {
                    // x = -1.0 or x = +1.0
                    if (yRawExp == 2047) {
                        // y is infinite
                        return Double.NaN;
                    } else {
                        // y is a large even integer
                        return 1.0;
                    }
                } else {
                    // the absolute value of x is either greater or smaller than 1.0

                    // if yRawExp == 2047 and mantissa is 0, y = -infinity or y = +infinity
                    // if 1085 < yRawExp < 2047, y is simply a large number, however, due to limited
                    // accuracy, at this magnitude it behaves just like infinity with regards to x
                    if ((y > 0) ^ (xRawExp < 1023)) {
                        // either y = +infinity (or large engouh) and abs(x) > 1.0
                        // or     y = -infinity (or large engouh) and abs(x) < 1.0
                        return Double.POSITIVE_INFINITY;
                    } else {
                        // either y = +infinity (or large engouh) and abs(x) < 1.0
                        // or     y = -infinity (or large engouh) and abs(x) > 1.0
                        return +0.0;
                    }
                }

            } else {
                // y is a regular non-zero number

                if (yRawExp >= 1023) {
                    // y may be an integral value, which should be handled specifically
                    final long yFullMantissa = IMPLICIT_HIGH_BIT | yRawMantissa;
                    if (yRawExp < 1075) {
                        // normal number with negative shift that may have a fractional part
                        final long integralMask = (-1L) << (1075 - yRawExp);
                        if ((yFullMantissa & integralMask) == yFullMantissa) {
                            // all fractional bits are 0, the number is really integral
                            final long l = yFullMantissa >> (1075 - yRawExp);
                            return FastMath.pow(x, (y < 0) ? -l : l);
                        }
                    } else {
                        // normal number with positive shift, always an integral value
                        // we know it fits in a primitive long because yRawExp > 1085 has been handled above
                        final long l = yFullMantissa << (yRawExp - 1075);
                        return FastMath.pow(x, (y < 0) ? -l : l);
                    }
                }

                // y is a non-integral value

                if (x == 0) {
                    // x = -0 or x = +0
                    // the integer powers have already been handled above
                    return y < 0 ? Double.POSITIVE_INFINITY : +0.0;
                } else if (xRawExp == 2047) {
                    if (xRawMantissa == 0) {
                        // x = -infinity or x = +infinity
                        return (y < 0) ? +0.0 : Double.POSITIVE_INFINITY;
                    } else {
                        // NaN
                        return Double.NaN;
                    }
                } else if (x < 0) {
                    // the integer powers have already been handled above
                    return Double.NaN;
                } else {

                    // this is the general case, for regular fractional numbers x and y

                    // Split y into ya and yb such that y = ya+yb
                    final double tmp = y * HEX_40000000;
                    final double ya = (y + tmp) - tmp;
                    final double yb = y - ya;

                    /* Compute ln(x) */
                    final double lns[] = new double[2];
                    final double lores = log(x, lns);
                    if (Double.isInfinite(lores)) { // don't allow this to be converted to NaN
                        return lores;
                    }

                    double lna = lns[0];
                    double lnb = lns[1];

                    /* resplit lns */
                    final double tmp1 = lna * HEX_40000000;
                    final double tmp2 = (lna + tmp1) - tmp1;
                    lnb += lna - tmp2;
                    lna = tmp2;

                    // y*ln(x) = (aa+ab)
                    final double aa = lna * ya;
                    final double ab = lna * yb + lnb * ya + lnb * yb;

                    lna = aa + ab;
                    lnb = -(lna - aa - ab);

                    double z = 1.0 / 120.0;
                    z = z * lnb + (1.0 / 24.0);
                    z = z * lnb + (1.0 / 6.0);
                    z = z * lnb + 0.5;
                    z = z * lnb + 1.0;
                    z *= lnb;

                    final double result = exp(lna, z, null);
                    //result = result + result * z;
                    return result;

                }
            }

        }
    }

    public static double pow(double d, long e) {
        if (e == 0) {
            return 1.0;
        } else if (e > 0) {
            return new Split(d).pow(e).full;
        } else {
            return new Split(d).reciprocal().pow(-e).full;
        }
    }

    private static double exp(double x, double extra, double[] hiPrec) {
        double intPartA;
        double intPartB;
        int intVal = (int) x;

        /* Lookup exp(floor(x)).
         * intPartA will have the upper 22 bits, intPartB will have the lower
         * 52 bits.
         */
        if (x < 0.0) {

            // We don't check against intVal here as conversion of large negative double values
            // may be affected by a JIT bug. Subsequent comparisons can safely use intVal
            if (x < -746d) {
                if (hiPrec != null) {
                    hiPrec[0] = 0.0;
                    hiPrec[1] = 0.0;
                }
                return 0.0;
            }

            if (intVal < -709) {
                /* This will produce a subnormal output */
                final double result = exp(x + 40.19140625, extra, hiPrec) / 285040095144011776.0;
                if (hiPrec != null) {
                    hiPrec[0] /= 285040095144011776.0;
                    hiPrec[1] /= 285040095144011776.0;
                }
                return result;
            }

            if (intVal == -709) {
                /* exp(1.494140625) is nearly a machine number... */
                final double result = exp(x + 1.494140625, extra, hiPrec) / 4.455505956692756620;
                if (hiPrec != null) {
                    hiPrec[0] /= 4.455505956692756620;
                    hiPrec[1] /= 4.455505956692756620;
                }
                return result;
            }

            intVal--;

        } else {
            if (intVal > 709) {
                if (hiPrec != null) {
                    hiPrec[0] = Double.POSITIVE_INFINITY;
                    hiPrec[1] = 0.0;
                }
                return Double.POSITIVE_INFINITY;
            }

        }

        intPartA = EXP_INT_TABLE_A[EXP_INT_TABLE_MAX_INDEX + intVal];
        intPartB = EXP_INT_TABLE_B[EXP_INT_TABLE_MAX_INDEX + intVal];

        /* Get the fractional part of x, find the greatest multiple of 2^-10 less than
         * x and look up the exp function of it.
         * fracPartA will have the upper 22 bits, fracPartB the lower 52 bits.
         */
        final int intFrac = (int) ((x - intVal) * 1024.0);
        final double fracPartA = EXP_FRAC_A[intFrac];
        final double fracPartB = EXP_FRAC_B[intFrac];

        /* epsilon is the difference in x from the nearest multiple of 2^-10.  It
         * has a value in the range 0 <= epsilon < 2^-10.
         * Do the subtraction from x as the last step to avoid possible loss of precision.
         */
        final double epsilon = x - (intVal + intFrac / 1024.0);

        /* Compute z = exp(epsilon) - 1.0 via a minimax polynomial.  z has
       full double precision (52 bits).  Since z < 2^-10, we will have
       62 bits of precision when combined with the constant 1.  This will be
       used in the last addition below to get proper rounding. */

        /* Remez generated polynomial.  Converges on the interval [0, 2^-10], error
       is less than 0.5 ULP */
        double z = 0.04168701738764507;
        z = z * epsilon + 0.1666666505023083;
        z = z * epsilon + 0.5000000000042687;
        z = z * epsilon + 1.0;
        z = z * epsilon + -3.940510424527919E-20;

        /* Compute (intPartA+intPartB) * (fracPartA+fracPartB) by binomial
       expansion.
       tempA is exact since intPartA and intPartB only have 22 bits each.
       tempB will have 52 bits of precision.
         */
        double tempA = intPartA * fracPartA;
        double tempB = intPartA * fracPartB + intPartB * fracPartA + intPartB * fracPartB;

        /* Compute the result.  (1+z)(tempA+tempB).  Order of operations is
       important.  For accuracy add by increasing size.  tempA is exact and
       much larger than the others.  If there are extra bits specified from the
       pow() function, use them. */
        final double tempC = tempB + tempA;

        // If tempC is positive infinite, the evaluation below could result in NaN,
        // because z could be negative at the same time.
        if (tempC == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }

        final double result;
        if (extra != 0.0) {
            result = tempC * extra * z + tempC * extra + tempC * z + tempB + tempA;
        } else {
            result = tempC * z + tempB + tempA;
        }

        if (hiPrec != null) {
            // If requesting high precision
            hiPrec[0] = tempA;
            hiPrec[1] = tempC * extra * z + tempC * extra + tempC * z + tempB;
        }

        return result;
    }

    private static double log(final double x, final double[] hiPrec) {
        if (x == 0) { // Handle special case of +0/-0
            return Double.NEGATIVE_INFINITY;
        }
        long bits = Double.doubleToRawLongBits(x);

        /* Handle special cases of negative input, and NaN */
        if (((bits & 0x8000000000000000L) != 0 || Double.isNaN(x)) && x != 0.0) {
            if (hiPrec != null) {
                hiPrec[0] = Double.NaN;
            }

            return Double.NaN;
        }

        /* Handle special cases of Positive infinity. */
        if (x == Double.POSITIVE_INFINITY) {
            if (hiPrec != null) {
                hiPrec[0] = Double.POSITIVE_INFINITY;
            }

            return Double.POSITIVE_INFINITY;
        }

        /* Extract the exponent */
        int exp = (int) (bits >> 52) - 1023;

        if ((bits & 0x7ff0000000000000L) == 0) {
            // Subnormal!
            if (x == 0) {
                // Zero
                if (hiPrec != null) {
                    hiPrec[0] = Double.NEGATIVE_INFINITY;
                }

                return Double.NEGATIVE_INFINITY;
            }

            /* Normalize the subnormal number. */
            bits <<= 1;
            while ((bits & 0x0010000000000000L) == 0) {
                --exp;
                bits <<= 1;
            }
        }


        if ((exp == -1 || exp == 0) && x < 1.01 && x > 0.99 && hiPrec == null) {
            /* The normal method doesn't work well in the range [0.99, 1.01], so call do a straight
           polynomial expansion in higer precision. */

            /* Compute x - 1.0 and split it */
            double xa = x - 1.0;
            double xb = xa - x + 1.0;
            double tmp = xa * HEX_40000000;
            double aa = xa + tmp - tmp;
            double ab = xa - aa;
            xa = aa;
            xb = ab;

            final double[] lnCoef_last = LN_QUICK_COEF[LN_QUICK_COEF.length - 1];
            double ya = lnCoef_last[0];
            double yb = lnCoef_last[1];

            for (int i = LN_QUICK_COEF.length - 2; i >= 0; i--) {
                /* Multiply a = y * x */
                aa = ya * xa;
                ab = ya * xb + yb * xa + yb * xb;
                /* split, so now y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;

                /* Add  a = y + lnQuickCoef */
                final double[] lnCoef_i = LN_QUICK_COEF[i];
                aa = ya + lnCoef_i[0];
                ab = yb + lnCoef_i[1];
                /* Split y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;
            }

            /* Multiply a = y * x */
            aa = ya * xa;
            ab = ya * xb + yb * xa + yb * xb;
            /* split, so now y = a */
            tmp = aa * HEX_40000000;
            ya = aa + tmp - tmp;
            yb = aa - ya + ab;

            return ya + yb;
        }

        // lnm is a log of a number in the range of 1.0 - 2.0, so 0 <= lnm < ln(2)
        final double[] lnm = LN_MANT[(int) ((bits & 0x000ffc0000000000L) >> 42)];

        /*
    double epsilon = x / Double.longBitsToDouble(bits & 0xfffffc0000000000L);

    epsilon -= 1.0;
         */

        // y is the most significant 10 bits of the mantissa
        //double y = Double.longBitsToDouble(bits & 0xfffffc0000000000L);
        //double epsilon = (x - y) / y;
        final double epsilon = (bits & 0x3ffffffffffL) / (TWO_POWER_52 + (bits & 0x000ffc0000000000L));

        double lnza = 0.0;
        double lnzb = 0.0;

        if (hiPrec != null) {
            /* split epsilon -> x */
            double tmp = epsilon * HEX_40000000;
            double aa = epsilon + tmp - tmp;
            double ab = epsilon - aa;
            double xa = aa;
            double xb = ab;

            /* Need a more accurate epsilon, so adjust the division. */
            final double numer = bits & 0x3ffffffffffL;
            final double denom = TWO_POWER_52 + (bits & 0x000ffc0000000000L);
            aa = numer - xa * denom - xb * denom;
            xb += aa / denom;

            /* Remez polynomial evaluation */
            final double[] lnCoef_last = LN_HI_PREC_COEF[LN_HI_PREC_COEF.length - 1];
            double ya = lnCoef_last[0];
            double yb = lnCoef_last[1];

            for (int i = LN_HI_PREC_COEF.length - 2; i >= 0; i--) {
                /* Multiply a = y * x */
                aa = ya * xa;
                ab = ya * xb + yb * xa + yb * xb;
                /* split, so now y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;

                /* Add  a = y + lnHiPrecCoef */
                final double[] lnCoef_i = LN_HI_PREC_COEF[i];
                aa = ya + lnCoef_i[0];
                ab = yb + lnCoef_i[1];
                /* Split y = a */
                tmp = aa * HEX_40000000;
                ya = aa + tmp - tmp;
                yb = aa - ya + ab;
            }

            /* Multiply a = y * x */
            aa = ya * xa;
            ab = ya * xb + yb * xa + yb * xb;

            /* split, so now lnz = a */
            /*
      tmp = aa * 1073741824.0;
      lnza = aa + tmp - tmp;
      lnzb = aa - lnza + ab;
             */
            lnza = aa + ab;
            lnzb = -(lnza - aa - ab);
        } else {
            /* High precision not required.  Eval Remez polynomial
         using standard double precision */
            lnza = -0.16624882440418567;
            lnza = lnza * epsilon + 0.19999954120254515;
            lnza = lnza * epsilon + -0.2499999997677497;
            lnza = lnza * epsilon + 0.3333333333332802;
            lnza = lnza * epsilon + -0.5;
            lnza = lnza * epsilon + 1.0;
            lnza *= epsilon;
        }

        /* Relative sizes:
         * lnzb     [0, 2.33E-10]
         * lnm[1]   [0, 1.17E-7]
         * ln2B*exp [0, 1.12E-4]
         * lnza      [0, 9.7E-4]
         * lnm[0]   [0, 0.692]
         * ln2A*exp [0, 709]
         */

        /* Compute the following sum:
         * lnzb + lnm[1] + ln2B*exp + lnza + lnm[0] + ln2A*exp;
         */

        //return lnzb + lnm[1] + ln2B*exp + lnza + lnm[0] + ln2A*exp;
        double a = LN_2_A * exp;
        double b = 0.0;
        double c = a + lnm[0];
        double d = -(c - a - lnm[0]);
        a = c;
        b += d;

        c = a + lnza;
        d = -(c - a - lnza);
        a = c;
        b += d;

        c = a + LN_2_B * exp;
        d = -(c - a - LN_2_B * exp);
        a = c;
        b += d;

        c = a + lnm[1];
        d = -(c - a - lnm[1]);
        a = c;
        b += d;

        c = a + lnzb;
        d = -(c - a - lnzb);
        a = c;
        b += d;

        if (hiPrec != null) {
            hiPrec[0] = a;
            hiPrec[1] = b;
        }

        return a + b;
    }

    private static class Split {

        /** Split version of NaN. */
        public static final Split NAN = new Split(Double.NaN, 0);

        /** Split version of positive infinity. */
        public static final Split POSITIVE_INFINITY = new Split(Double.POSITIVE_INFINITY, 0);

        /** Split version of negative infinity. */
        public static final Split NEGATIVE_INFINITY = new Split(Double.NEGATIVE_INFINITY, 0);

        /** Full number. */
        private final double full;

        /** High order bits. */
        private final double high;

        /** Low order bits. */
        private final double low;

        /** Simple constructor.
         * @param x number to split
         */
        Split(final double x) {
            full = x;
            high = Double.longBitsToDouble(Double.doubleToRawLongBits(x) & ((-1L) << 27));
            low  = x - high;
        }

        /** Simple constructor.
         * @param high high order bits
         * @param low low order bits
         */
        Split(final double high, final double low) {
            this(high == 0.0 ? (low == 0.0 && Double.doubleToRawLongBits(high) == Long.MIN_VALUE /* negative zero */ ? -0.0 : low) : high + low, high, low);
        }

        /** Simple constructor.
         * @param full full number
         * @param high high order bits
         * @param low low order bits
         */
        Split(final double full, final double high, final double low) {
            this.full = full;
            this.high = high;
            this.low  = low;
        }

        /** Multiply the instance by another one.
         * @param b other instance to multiply by
         * @return product
         */
        public Split multiply(final Split b) {
            // beware the following expressions must NOT be simplified, they rely on floating point arithmetic properties
            final Split  mulBasic  = new Split(full * b.full);
            final double mulError  = low * b.low - (((mulBasic.full - high * b.high) - low * b.high) - high * b.low);
            return new Split(mulBasic.high, mulBasic.low + mulError);
        }

        /** Compute the reciprocal of the instance.
         * @return reciprocal of the instance
         */
        public Split reciprocal() {

            final double approximateInv = 1.0 / full;
            final Split  splitInv       = new Split(approximateInv);

            // if 1.0/d were computed perfectly, remultiplying it by d should give 1.0
            // we want to estimate the error so we can fix the low order bits of approximateInvLow
            // beware the following expressions must NOT be simplified, they rely on floating point arithmetic properties
            final Split product = multiply(splitInv);
            final double error  = (product.high - 1) + product.low;

            // better accuracy estimate of reciprocal
            return Double.isNaN(error) ? splitInv : new Split(splitInv.high, splitInv.low - error / full);

        }

        /** Computes this^e.
         * @param e exponent (beware, here it MUST be > 0; the only exclusion is Long.MIN_VALUE)
         * @return d^e, split in high and low bits
         * @since 3.6
         */
        private Split pow(final long e) {

            // prepare result
            Split result = new Split(1);

            // d^(2p)
            Split d2p = new Split(full, high, low);

            for (long p = e; p != 0; p >>>= 1) {

                if ((p & 0x1) != 0) {
                    // accurate multiplication result = result * d^(2p) using Veltkamp TwoProduct algorithm
                    result = result.multiply(d2p);
                }

                // accurate squaring d^(2(p+1)) = d^(2p) * d^(2p) using Veltkamp TwoProduct algorithm
                d2p = d2p.multiply(d2p);

            }

            if (Double.isNaN(result.full)) {
                if (Double.isNaN(full)) {
                    return Split.NAN;
                } else {
                    // some intermediate numbers exceeded capacity,
                    // and the low order bits became NaN (because infinity - infinity = NaN)
                    if (Math.abs(full) < 1) {
                        return new Split(Math.copySign(0.0, full), 0.0);
                    } else if (full < 0 && (e & 0x1) == 1) {
                        return Split.NEGATIVE_INFINITY;
                    } else {
                        return Split.POSITIVE_INFINITY;
                    }
                }
            } else {
                return result;
            }

        }

    }

    private static final double[][] LN_QUICK_COEF = {
            {1.0, 5.669184079525E-24},
            {-0.25, -0.25},
            {0.3333333134651184, 1.986821492305628E-8},
            {-0.25, -6.663542893624021E-14},
            {0.19999998807907104, 1.1921056801463227E-8},
            {-0.1666666567325592, -7.800414592973399E-9},
            {0.1428571343421936, 5.650007086920087E-9},
            {-0.12502530217170715, -7.44321345601866E-11},
            {0.11113807559013367, 9.219544613762692E-9},
    };

    private static final double[][] LN_HI_PREC_COEF = {
            {1.0, -6.032174644509064E-23},
            {-0.25, -0.25},
            {0.3333333134651184, 1.9868161777724352E-8},
            {-0.2499999701976776, -2.957007209750105E-8},
            {0.19999954104423523, 1.5830993332061267E-10},
            {-0.16624879837036133, -2.6033824355191673E-8}
    };

    private static final double[] EXP_INT_TABLE_A = new double[]{
            +0.0d,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            +1.2167807682331913E-308d,
            +3.3075532478807267E-308d,
            +8.990862214387203E-308d,
            +2.4439696075216986E-307d,
            +6.64339758024534E-307d,
            +1.8058628951432254E-306d,
            +4.908843759498681E-306d,
            +1.334362017065677E-305d,
            +3.627172425759641E-305d,
            +9.85967600992008E-305d,
            +2.680137967689915E-304d,
            +7.285370725133842E-304d,
            +1.9803689272433392E-303d,
            +5.3832011494782624E-303d,
            +1.463305638201413E-302d,
            +3.9776772027043775E-302d,
            +1.0812448255518705E-301d,
            +2.9391280956327795E-301d,
            +7.989378677301346E-301d,
            +2.1717383041010577E-300d,
            +5.903396499766243E-300d,
            +1.604709595901607E-299d,
            +4.3620527352131126E-299d,
            +1.1857289715706991E-298d,
            +3.2231452986239366E-298d,
            +8.761416875971053E-298d,
            +2.381600167287677E-297d,
            +6.473860152384321E-297d,
            +1.7597776278732318E-296d,
            +4.7835721669653157E-296d,
            +1.3003096668152053E-295d,
            +3.5346080979652066E-295d,
            +9.608060944124859E-295d,
            +2.6117415961302846E-294d,
            +7.099449830809996E-294d,
            +1.9298305829106006E-293d,
            +5.245823134132673E-293d,
            +1.4259627797225802E-292d,
            +3.8761686729764145E-292d,
            +1.0536518897078156E-291d,
            +2.864122672853628E-291d,
            +7.785491934690374E-291d,
            +2.116316283183901E-290d,
            +5.7527436249968E-290d,
            +1.5637579898345352E-289d,
            +4.250734424415339E-289d,
            +1.1554696041977512E-288d,
            +3.1408919441362495E-288d,
            +8.537829238438662E-288d,
            +2.320822576772103E-287d,
            +6.308649765138419E-287d,
            +1.7148689119310826E-286d,
            +4.66149719271323E-286d,
            +1.267126226441217E-285d,
            +3.444406231880653E-285d,
            +9.362866914115166E-285d,
            +2.5450911557068313E-284d,
            +6.918275021321188E-284d,
            +1.880582039589629E-283d,
            +5.111952261540649E-283d,
            +1.3895726688907995E-282d,
            +3.7772500667438066E-282d,
            +1.026763015362553E-281d,
            +2.791031173360063E-281d,
            +7.586808748646825E-281d,
            +2.0623086887184633E-280d,
            +5.605936171588964E-280d,
            +1.5238514098804918E-279d,
            +4.1422578754033235E-279d,
            +1.1259823210174452E-278d,
            +3.060737220976933E-278d,
            +8.319947089683576E-278d,
            +2.2615958035357106E-277d,
            +6.147655179898435E-277d,
            +1.6711060014400145E-276d,
            +4.542536646012133E-276d,
            +1.2347896500246374E-275d,
            +3.3565057475434694E-275d,
            +9.123929070778758E-275d,
            +2.4801413921885483E-274d,
            +6.741722283079056E-274d,
            +1.8325902719086093E-273d,
            +4.981496462621207E-273d,
            +1.3541112064618357E-272d,
            +3.68085620656127E-272d,
            +1.0005602916630382E-271d,
            +2.719805132368625E-271d,
            +7.393196131284108E-271d,
            +2.0096791226867E-270d,
            +5.462874707256208E-270d,
            +1.4849631831943512E-269d,
            +4.036548930895323E-269d,
            +1.0972476870931676E-268d,
            +2.9826282194717127E-268d,
            +8.107624153838987E-268d,
            +2.2038806519542315E-267d,
            +5.990769236615968E-267d,
            +1.628459873440512E-266d,
            +4.4266130556431266E-266d,
            +1.203278237867575E-265d,
            +3.270849446965521E-265d,
            +8.891090288030614E-265d,
            +2.4168487931443637E-264d,
            +6.569676185250389E-264d,
            +1.7858231429575898E-263d,
            +4.85437090269903E-263d,
            +1.3195548295785448E-262d,
            +3.5869215528816054E-262d,
            +9.750264097807267E-262d,
            +2.650396454019762E-261d,
            +7.204525142098426E-261d,
            +1.958392846081373E-260d,
            +5.32346341339996E-260d,
            +1.4470673509275515E-259d,
            +3.9335373658569176E-259d,
            +1.0692462289051038E-258d,
            +2.9065128598079075E-258d,
            +7.900720862969045E-258d,
            +2.147638465376883E-257d,
            +5.8378869339035456E-257d,
            +1.5869022483809747E-256d,
            +4.3136475849391444E-256d,
            +1.1725710340687719E-255d,
            +3.1873780814410126E-255d,
            +8.66419234315257E-255d,
            +2.35517168886351E-254d,
            +6.402020300783889E-254d,
            +1.740249660600677E-253d,
            +4.7304887145310405E-253d,
            +1.2858802448614707E-252d,
            +3.495384792953975E-252d,
            +9.501439740542955E-252d,
            +2.582759362004277E-251d,
            +7.020668578160457E-251d,
            +1.908415302517694E-250d,
            +5.1876107490791666E-250d,
            +1.4101386971763257E-249d,
            +3.8331545111676784E-249d,
            +1.0419594359065132E-248d,
            +2.8323395451363237E-248d,
            +7.699097067385825E-248d,
            +2.0928317096428755E-247d,
            +5.688906371296133E-247d,
            +1.5464049837965422E-246d,
            +4.2035646586788297E-246d,
            +1.1426473877336358E-245d,
            +3.106037603716254E-245d,
            +8.443084996839363E-245d,
            +2.2950686306677644E-244d,
            +6.238642390386363E-244d,
            +1.695838923802857E-243d,
            +4.6097680405580995E-243d,
            +1.2530649392922358E-242d,
            +3.4061835424180075E-242d,
            +9.25896798127602E-242d,
            +2.5168480541429286E-241d,
            +6.841502859109196E-241d,
            +1.8597132378953187E-240d,
            +5.055224959032211E-240d,
            +1.374152583940637E-239d,
            +3.735333866258403E-239d,
            +1.0153690688015855E-238d,
            +2.7600590782738726E-238d,
            +7.502618487550056E-238d,
            +2.0394233446495043E-237d,
            +5.543727690168612E-237d,
            +1.5069412868172555E-236d,
            +4.0962906236847E-236d,
            +1.1134873918971586E-235d,
            +3.026772467749944E-235d,
            +8.227620163729258E-235d,
            +2.2364990583200056E-234d,
            +6.079434951446575E-234d,
            +1.6525617499662284E-233d,
            +4.4921289690525345E-233d,
            +1.2210872189854344E-232d,
            +3.3192593301633E-232d,
            +9.02268127425393E-232d,
            +2.4526190464373087E-231d,
            +6.666909874218774E-231d,
            +1.8122539547625083E-230d,
            +4.926216840507529E-230d,
            +1.3390847149416908E-229d,
            +3.6400093808551196E-229d,
            +9.894571625944288E-229d,
            +2.689623698321582E-228d,
            +7.31115423069187E-228d,
            +1.9873779569310022E-227d,
            +5.402252865260326E-227d,
            +1.4684846983789053E-226d,
            +3.991755413823315E-226d,
            +1.0850715739509136E-225d,
            +2.9495302004590423E-225d,
            +8.017654713159388E-225d,
            +2.179424521221378E-224d,
            +5.924290380648597E-224d,
            +1.6103890140790331E-223d,
            +4.377491272857675E-223d,
            +1.1899254154663847E-222d,
            +3.2345523990372546E-222d,
            +8.792425221770645E-222d,
            +2.3900289095512176E-221d,
            +6.496772856703278E-221d,
            +1.7660059778220905E-220d,
            +4.800501435803201E-220d,
            +1.3049116216750674E-219d,
            +3.5471180281159325E-219d,
            +9.642065709892252E-219d,
            +2.6209850274990846E-218d,
            +7.124574366530717E-218d,
            +1.9366601417010147E-217d,
            +5.264388476949737E-217d,
            +1.431009021985696E-216d,
            +3.889885799962507E-216d,
            +1.057380684430436E-215d,
            +2.8742587656021775E-215d,
            +7.813044552050569E-215d,
            +2.1238058974550874E-214d,
            +5.773102661099307E-214d,
            +1.5692921723471877E-213d,
            +4.2657777816050375E-213d,
            +1.1595585743839232E-212d,
            +3.1520070828798975E-212d,
            +8.568043768122183E-212d,
            +2.329035966595791E-211d,
            +6.33097561889469E-211d,
            +1.720937714565362E-210d,
            +4.677993239821998E-210d,
            +1.2716105485691878E-209d,
            +3.456595573934475E-209d,
            +9.396000024637834E-209d,
            +2.55409795397022E-208d,
            +6.942757623821567E-208d,
            +1.887237361505784E-207d,
            +5.13004286606108E-207d,
            +1.3944901709366118E-206d,
            +3.7906173667738715E-206d,
            +1.0303966192973381E-205d,
            +2.8009086220877197E-205d,
            +7.613657850210907E-205d,
            +2.0696069842597556E-204d,
            +5.6257755605305175E-204d,
            +1.5292444435954893E-203d,
            +4.156916476922876E-203d,
            +1.12996721591364E-202d,
            +3.071569248856111E-202d,
            +8.349390727162016E-202d,
            +2.2695999828608633E-201d,
            +6.1694117899971836E-201d,
            +1.677020107827128E-200d,
            +4.558612479525779E-200d,
            +1.2391595516612638E-199d,
            +3.3683846288580648E-199d,
            +9.156218120779494E-199d,
            +2.4889182184335247E-198d,
            +6.765580431441772E-198d,
            +1.839075686473352E-197d,
            +4.999126524757713E-197d,
            +1.3589033107846643E-196d,
            +3.6938826366068014E-196d,
            +1.0041012794280992E-195d,
            +2.7294301888986675E-195d,
            +7.419361045185406E-195d,
            +2.016791373353671E-194d,
            +5.482208065983983E-194d,
            +1.490218341008089E-193d,
            +4.050833763855709E-193d,
            +1.101130773265179E-192d,
            +2.993183789477209E-192d,
            +8.136316299122392E-192d,
            +2.2116799789922265E-191d,
            +6.011969568315371E-191d,
            +1.6342228966392253E-190d,
            +4.4422779589171113E-190d,
            +1.2075364784547675E-189d,
            +3.282424571107068E-189d,
            +8.92255448602772E-189d,
            +2.425402115319395E-188d,
            +6.592926904915355E-188d,
            +1.79214305133496E-187d,
            +4.871550528055661E-187d,
            +1.3242245776666673E-186d,
            +3.599615946028287E-186d,
            +9.78476998200719E-186d,
            +2.659776075359514E-185d,
            +7.230020851688713E-185d,
            +1.9653234116333892E-184d,
            +5.34230278107224E-184d,
            +1.4521887058451231E-183d,
            +3.947457923821984E-183d,
            +1.0730302255093144E-182d,
            +2.9167986204137332E-182d,
            +7.928680793406766E-182d,
            +2.1552386987482013E-181d,
            +5.858546779607288E-181d,
            +1.5925182066949723E-180d,
            +4.328913614497258E-180d,
            +1.1767205227552116E-179d,
            +3.198658219194836E-179d,
            +8.694853785564504E-179d,
            +2.363506255864984E-178d,
            +6.42467573615509E-178d,
            +1.746408207555959E-177d,
            +4.747229597770176E-177d,
            +1.2904307529671472E-176d,
            +3.507754341050756E-176d,
            +9.535066345267336E-176d,
            +2.591899541396432E-175d,
            +7.045512786902009E-175d,
            +1.9151693415969248E-174d,
            +5.205969622575851E-174d,
            +1.4151292367806538E-173d,
            +3.846720258072078E-173d,
            +1.045647032279984E-172d,
            +2.8423629805010285E-172d,
            +7.726344058192276E-172d,
            +2.1002377128928765E-171d,
            +5.709039546124285E-171d,
            +1.5518778128928824E-170d,
            +4.218440703602533E-170d,
            +1.1466910691560932E-169d,
            +3.1170298734336303E-169d,
            +8.472965161251656E-169d,
            +2.303190374523956E-168d,
            +6.260720440258473E-168d,
            +1.701840523821621E-167d,
            +4.62608152166211E-167d,
            +1.2574995962791943E-166d,
            +3.418237608335161E-166d,
            +9.29173407843235E-166d,
            +2.5257552661512635E-165d,
            +6.865714679174435E-165d,
            +1.866294830116931E-164d,
            +5.073114566291778E-164d,
            +1.3790154522394582E-163d,
            +3.7485528226129495E-163d,
            +1.0189624503698769E-162d,
            +2.7698267293941856E-162d,
            +7.529170882336924E-162d,
            +2.0466404088178596E-161d,
            +5.56334611651382E-161d,
            +1.512274346576166E-160d,
            +4.110787043867721E-160d,
            +1.1174279267498045E-159d,
            +3.0374839443564585E-159d,
            +8.25673801176584E-159d,
            +2.244414150254963E-158d,
            +6.1009492034592176E-158d,
            +1.6584100275603453E-157d,
            +4.50802633729044E-157d,
            +1.2254085656601853E-156d,
            +3.3310057014599044E-156d,
            +9.054612259832416E-156d,
            +2.4612985502035675E-155d,
            +6.690503835950083E-155d,
            +1.8186679660152888E-154d,
            +4.9436516047443576E-154d,
            +1.3438240331106108E-153d,
            +3.652892398145774E-153d,
            +9.92958982547828E-153d,
            +2.6991427376823027E-152d,
            +7.3370297995122135E-152d,
            +1.994411660450821E-151d,
            +5.421372463189529E-151d,
            +1.4736818914204564E-150d,
            +4.005882964287806E-150d,
            +1.088911919926534E-149d,
            +2.9599693109692324E-149d,
            +8.046030012041041E-149d,
            +2.18713790898745E-148d,
            +5.945256705384597E-148d,
            +1.6160884846515524E-147d,
            +4.392983574030969E-147d,
            +1.1941366764543551E-146d,
            +3.2460001983475855E-146d,
            +8.8235440586675E-146d,
            +2.3984878190403553E-145d,
            +6.519765758635405E-145d,
            +1.772256261139753E-144d,
            +4.817491674217065E-144d,
            +1.3095299991573769E-143d,
            +3.559671483107555E-143d,
            +9.676190774054103E-143d,
            +2.630261301303634E-142d,
            +7.149792225695347E-142d,
            +1.943514969662872E-141d,
            +5.283020542151163E-141d,
            +1.4360739330834996E-140d,
            +3.9036541111764032E-140d,
            +1.0611230602364477E-139d,
            +2.8844319473099593E-139d,
            +7.84069876400596E-139d,
            +2.1313228444765414E-138d,
            +5.793536445518422E-138d,
            +1.5748463788034308E-137d,
            +4.2808762411845363E-137d,
            +1.1636629220608724E-136d,
            +3.163163464591171E-136d,
            +8.598369704466743E-136d,
            +2.337279322276433E-135d,
            +6.353384093665193E-135d,
            +1.7270287031459572E-134d,
            +4.694550492773212E-134d,
            +1.2761111606368036E-133d,
            +3.4688299108856403E-133d,
            +9.429257929713919E-133d,
            +2.5631381141873417E-132d,
            +6.967331001069377E-132d,
            +1.8939170679975288E-131d,
            +5.148199748336684E-131d,
            +1.3994258162094293E-130d,
            +3.804034213613942E-130d,
            +1.0340436948077763E-129d,
            +2.8108219632627907E-129d,
            +7.640606938467665E-129d,
            +2.0769322678328357E-128d,
            +5.645687086879944E-128d,
            +1.5346568127351796E-127d,
            +4.171630237420918E-127d,
            +1.1339665711932977E-126d,
            +3.0824406750909563E-126d,
            +8.37894218404787E-126d,
            +2.2776327994966818E-125d,
            +6.191247522703296E-125d,
            +1.6829556040859853E-124d,
            +4.5747479502862494E-124d,
            +1.2435453481209945E-123d,
            +3.3803067202247166E-123d,
            +9.188625696750548E-123d,
            +2.4977273040076145E-122d,
            +6.789527378582775E-122d,
            +1.845584943222965E-121d,
            +5.016820182185716E-121d,
            +1.3637129731022491E-120d,
            +3.706956710275979E-120d,
            +1.0076552294433743E-119d,
            +2.739090595934893E-119d,
            +7.445620503219039E-119d,
            +2.023929422267303E-118d,
            +5.501611507503037E-118d,
            +1.4954928881576769E-117d,
            +4.0651709187617596E-117d,
            +1.1050280679513555E-116d,
            +3.003777734030334E-116d,
            +8.165114384910189E-116d,
            +2.219508285637377E-115d,
            +6.033249389304709E-115d,
            +1.6400070480930697E-114d,
            +4.458001565878111E-114d,
            +1.2118105325725891E-113d,
            +3.2940421731384895E-113d,
            +8.954135150208654E-113d,
            +2.433986351722258E-112d,
            +6.616260705434716E-112d,
            +1.7984863104885375E-111d,
            +4.888792154132158E-111d,
            +1.3289115531074511E-110d,
            +3.612356038181234E-110d,
            +9.819402293160495E-110d,
            +2.6691899766673256E-109d,
            +7.255611264437603E-109d,
            +1.9722796756250217E-108d,
            +5.361211684173837E-108d,
            +1.4573285967670963E-107d,
            +3.961429477016909E-107d,
            +1.0768281419102595E-106d,
            +2.9271223293841774E-106d,
            +7.956744351476403E-106d,
            +2.1628672925745152E-105d,
            +5.879282834821692E-105d,
            +1.5981547034872092E-104d,
            +4.344234755347641E-104d,
            +1.1808855501885005E-103d,
            +3.2099795870407646E-103d,
            +8.725629524586503E-103d,
            +2.3718718327094683E-102d,
            +6.44741641521183E-102d,
            +1.7525895549820557E-101d,
            +4.7640323331013947E-101d,
            +1.2949980563724296E-100d,
            +3.5201699899499525E-100d,
            +9.56881327374431E-100d,
            +2.6010732940533088E-99d,
            +7.070450309820548E-99d,
            +1.9219478787856753E-98d,
            +5.2243955659975294E-98d,
            +1.4201378353978042E-97d,
            +3.8603349913851996E-97d,
            +1.0493479260117497E-96d,
            +2.8524232604238555E-96d,
            +7.753690709912764E-96d,
            +2.1076716069929933E-95d,
            +5.72924572981599E-95d,
            +1.5573703263204683E-94d,
            +4.233371554108682E-94d,
            +1.1507496472539512E-93d,
            +3.1280620563875923E-93d,
            +8.5029538631631E-93d,
            +2.3113425190436427E-92d,
            +6.28287989314225E-92d,
            +1.7078641226055994E-91d,
            +4.6424556110307644E-91d,
            +1.261950308999819E-90d,
            +3.430336362898836E-90d,
            +9.324622137237299E-90d,
            +2.5346947846365435E-89d,
            +6.890014851450124E-89d,
            +1.8729003560057785E-88d,
            +5.091070300111434E-88d,
            +1.3838964592430477E-87d,
            +3.761820584522275E-87d,
            +1.0225689628581036E-86d,
            +2.7796303536272215E-86d,
            +7.555818934379333E-86d,
            +2.053884626293416E-85d,
            +5.583037134407759E-85d,
            +1.5176268538776042E-84d,
            +4.125337057189083E-84d,
            +1.121383042095528E-83d,
            +3.0482348236054953E-83d,
            +8.285962249116636E-83d,
            +2.2523580600947705E-82d,
            +6.122543452787843E-82d,
            +1.664279766968299E-81d,
            +4.523982262003404E-81d,
            +1.2297456769063303E-80d,
            +3.342795345742034E-80d,
            +9.086660081726823E-80d,
            +2.4700104681773258E-79d,
            +6.714184569587689E-79d,
            +1.8251046352720517E-78d,
            +4.961148056969105E-78d,
            +1.3485799924445315E-77d,
            +3.665820371396835E-77d,
            +9.964732578705785E-77d,
            +2.708695208461993E-76d,
            +7.362996533913695E-76d,
            +2.0014700145557332E-75d,
            +5.440559532453721E-75d,
            +1.4788974793889734E-74d,
            +4.020060558571273E-74d,
            +1.092765612182012E-73d,
            +2.970445258959489E-73d,
            +8.074507236705857E-73d,
            +2.1948784599535102E-72d,
            +5.966298125808066E-72d,
            +1.6218081151910012E-71d,
            +4.408531734441582E-71d,
            +1.198363039426718E-70d,
            +3.257488853378793E-70d,
            +8.854771398921902E-70d,
            +2.406976727302894E-69d,
            +6.542840888268955E-69d,
            +1.778528517418201E-68d,
            +4.834541417183388E-68d,
            +1.3141647465063647E-67d,
            +3.572270133517001E-67d,
            +9.710435805122717E-67d,
            +2.63957027915428E-66d,
            +7.175096392165733E-66d,
            +1.9503931430716318E-65d,
            +5.3017188565638215E-65d,
            +1.4411566290936352E-64d,
            +3.9174693825966044E-64d,
            +1.0648786018364265E-63d,
            +2.8946401383311E-63d,
            +7.868447965383903E-63d,
            +2.1388659707647114E-62d,
            +5.814040618670345E-62d,
            +1.5804200403673568E-61d,
            +4.296027044486766E-61d,
            +1.1677812418806031E-60d,
            +3.174358801839755E-60d,
            +8.62880163941313E-60d,
            +2.345551464945955E-59d,
            +6.3758692300917355E-59d,
            +1.733140900346534E-58d,
            +4.711165925070571E-58d,
            +1.2806275683797178E-57d,
            +3.481106736845E-57d,
            +9.462629520363307E-57d,
            +2.5722094667974783E-56d,
            +6.9919903587080315E-56d,
            +1.9006201022568844E-55d,
            +5.166420404109835E-55d,
            +1.4043786616805493E-54d,
            +3.8174968984748894E-54d,
            +1.03770335512154E-53d,
            +2.820769858672565E-53d,
            +7.667647949477605E-53d,
            +2.0842827711783212E-52d,
            +5.6656680900216754E-52d,
            +1.5400881501571645E-51d,
            +4.1863938339341257E-51d,
            +1.1379799629071911E-50d,
            +3.093350150840571E-50d,
            +8.408597060399334E-50d,
            +2.2856938448387544E-49d,
            +6.2131591878042886E-49d,
            +1.688911928929718E-48d,
            +4.5909386437919143E-48d,
            +1.2479464696643861E-47d,
            +3.3922703599272275E-47d,
            +9.221146830884422E-47d,
            +2.5065676066043174E-46d,
            +6.8135571305481364E-46d,
            +1.8521166948363666E-45d,
            +5.0345752964740226E-45d,
            +1.368539456379101E-44d,
            +3.720075801577098E-44d,
            +1.0112214979786464E-43d,
            +2.7487849807248755E-43d,
            +7.47197247068667E-43d,
            +2.0310928323153876E-42d,
            +5.521082422279256E-42d,
            +1.5007857288519654E-41d,
            +4.0795586181406803E-41d,
            +1.108938997126179E-40d,
            +3.0144088843073416E-40d,
            +8.194012195477669E-40d,
            +2.2273635587196807E-39d,
            +6.054601485195952E-39d,
            +1.6458113136245473E-38d,
            +4.473779311490168E-38d,
            +1.2160992719555806E-37d,
            +3.3057007442449645E-37d,
            +8.985825281444118E-37d,
            +2.442600707513088E-36d,
            +6.639677673630215E-36d,
            +1.8048513285848406E-35d,
            +4.906094420881007E-35d,
            +1.3336148713971936E-34d,
            +3.625141007634431E-34d,
            +9.854154449263851E-34d,
            +2.6786368134431636E-33d,
            +7.28128971953363E-33d,
            +1.9792597720953414E-32d,
            +5.380185921962174E-32d,
            +1.4624861244004054E-31d,
            +3.975449484028966E-31d,
            +1.080639291795678E-30d,
            +2.9374821418009058E-30d,
            +7.984904044796711E-30d,
            +2.1705221445447534E-29d,
            +5.900089995748943E-29d,
            +1.6038109389511792E-28d,
            +4.359610133382778E-28d,
            +1.185064946717304E-27d,
            +3.221340469489223E-27d,
            +8.756510122348782E-27d,
            +2.380266370880709E-26d,
            +6.47023467943241E-26d,
            +1.75879225876483E-25d,
            +4.780892502168074E-25d,
            +1.2995814853898995E-24d,
            +3.5326287852455166E-24d,
            +9.602680736954162E-24d,
            +2.6102792042257208E-23d,
            +7.095474414148981E-23d,
            +1.9287497671359936E-22d,
            +5.242885191553114E-22d,
            +1.4251641388208515E-21d,
            +3.873997809109103E-21d,
            +1.0530616658562386E-20d,
            +2.862518609581133E-20d,
            +7.78113163345177E-20d,
            +2.1151310700892382E-19d,
            +5.74952254077566E-19d,
            +1.5628822871880503E-18d,
            +4.24835413113866E-18d,
            +1.1548223864099742E-17d,
            +3.139132557537509E-17d,
            +8.533046968331264E-17d,
            +2.3195229636950566E-16d,
            +6.305116324200775E-16d,
            +1.71390848833098E-15d,
            +4.6588861918718874E-15d,
            +1.2664165777252073E-14d,
            +3.442477422913037E-14d,
            +9.357622912219837E-14d,
            +2.5436656904062604E-13d,
            +6.914399608426436E-13d,
            +1.879528650772233E-12d,
            +5.1090893668503945E-12d,
            +1.3887944613766301E-11d,
            +3.775134371775124E-11d,
            +1.0261880234452292E-10d,
            +2.789468100949932E-10d,
            +7.582560135332983E-10d,
            +2.061153470123145E-9d,
            +5.602796449011294E-9d,
            +1.5229979055675358E-8d,
            +4.139937459513021E-8d,
            +1.1253517584464134E-7d,
            +3.059023470086686E-7d,
            +8.315287232107949E-7d,
            +2.260329438286135E-6d,
            +6.1442124206223525E-6d,
            +1.670170240686275E-5d,
            +4.539993096841499E-5d,
            +1.2340981629677117E-4d,
            +3.35462624207139E-4d,
            +9.118819143623114E-4d,
            +0.0024787522852420807d,
            +0.006737947463989258d,
            +0.018315639346837997d,
            +0.049787066876888275d,
            +0.1353352963924408d,
            +0.3678794503211975d,
            +1.0d,
            +2.7182817459106445d,
            +7.389056205749512d,
            +20.08553695678711d,
            +54.59815216064453d,
            +148.41314697265625d,
            +403.42877197265625d,
            +1096.633056640625d,
            +2980.9580078125d,
            +8103.083984375d,
            +22026.46484375d,
            +59874.140625d,
            +162754.78125d,
            +442413.375d,
            +1202604.25d,
            +3269017.5d,
            +8886110.0d,
            +2.4154952E7d,
            +6.5659968E7d,
            +1.78482304E8d,
            +4.85165184E8d,
            +1.318815744E9d,
            +3.584912896E9d,
            +9.74480384E9d,
            +2.6489122816E10d,
            +7.200489472E10d,
            +1.95729620992E11d,
            +5.32048248832E11d,
            +1.446257098752E12d,
            +3.9313342464E12d,
            +1.0686474223616E13d,
            +2.904884772864E13d,
            +7.8962956959744E13d,
            +2.14643574308864E14d,
            +5.83461777702912E14d,
            +1.586013579247616E15d,
            +4.31123180027904E15d,
            +1.1719142537166848E16d,
            +3.1855931348221952E16d,
            +8.6593395455164416E16d,
            +2.35385270340419584E17d,
            +6.3984347447610573E17d,
            +1.73927483790327808E18d,
            +4.7278395262972723E18d,
            +1.285159987981792E19d,
            +3.493427277593156E19d,
            +9.496119530068797E19d,
            +2.581312717296228E20d,
            +7.016736290557636E20d,
            +1.907346499785443E21d,
            +5.1847060206155E21d,
            +1.4093490364499379E22d,
            +3.831007739580998E22d,
            +1.0413759887481643E23d,
            +2.8307533984544136E23d,
            +7.694785471490595E23d,
            +2.0916595931561093E24d,
            +5.685720022003016E24d,
            +1.545539007875769E25d,
            +4.201209991636407E25d,
            +1.142007304008196E26d,
            +3.104297782658242E26d,
            +8.43835682327257E26d,
            +2.2937832658080656E27d,
            +6.23514943204966E27d,
            +1.694889206675675E28d,
            +4.607187019879158E28d,
            +1.2523630909973607E29d,
            +3.4042761729010895E29d,
            +9.253781621373885E29d,
            +2.5154385492401904E30d,
            +6.837671137556327E30d,
            +1.8586717056324128E31d,
            +5.05239404378821E31d,
            +1.3733830589835937E32d,
            +3.733241849647479E32d,
            +1.014800418749161E33d,
            +2.758513549969986E33d,
            +7.498416981578345E33d,
            +2.0382811492597872E34d,
            +5.540622484676759E34d,
            +1.5060972626944096E35d,
            +4.0939972479624634E35d,
            +1.1128638067747114E36d,
            +3.0250770246136387E36d,
            +8.223012393018281E36d,
            +2.2352467822017166E37d,
            +6.076029840339376E37d,
            +1.6516361647240826E38d,
            +4.4896127778163155E38d,
            +1.2204032949639917E39d,
            +3.3174000012927697E39d,
            +9.017628107716908E39d,
            +2.451245443147225E40d,
            +6.663175904917432E40d,
            +1.8112388823726723E41d,
            +4.923458004084836E41d,
            +1.3383347029375378E42d,
            +3.637970747803715E42d,
            +9.889030935681123E42d,
            +2.6881169167589747E43d,
            +7.307059786371152E43d,
            +1.986264756071962E44d,
            +5.399227989109673E44d,
            +1.467662348860426E45d,
            +3.989519470441919E45d,
            +1.0844638420493122E46d,
            +2.9478781225754055E46d,
            +8.013164089994031E46d,
            +2.1782039447564253E47d,
            +5.920972420778763E47d,
            +1.609486943324346E48d,
            +4.3750396394525074E48d,
            +1.1892591576149107E49d,
            +3.2327411123173475E49d,
            +8.787501601904039E49d,
            +2.3886908001521312E50d,
            +6.493134033643613E50d,
            +1.7650169203544438E51d,
            +4.7978130078372714E51d,
            +1.3041809768060802E52d,
            +3.5451314095271004E52d,
            +9.636666808527841E52d,
            +2.6195174357581655E53d,
            +7.120586694432509E53d,
            +1.9355758655647052E54d,
            +5.2614409704305464E54d,
            +1.4302079642723736E55d,
            +3.8877083524279136E55d,
            +1.0567886837680406E56d,
            +2.872649515690124E56d,
            +7.808670894670738E56d,
            +2.1226166967029073E57d,
            +5.769871153180574E57d,
            +1.568413405104933E58d,
            +4.263390023436419E58d,
            +1.1589095247718807E59d,
            +3.150242850860434E59d,
            +8.563247933339596E59d,
            +2.3277319969498524E60d,
            +6.327431953939798E60d,
            +1.719974302355042E61d,
            +4.675374788964851E61d,
            +1.2708985520400816E62d,
            +3.454660807101683E62d,
            +9.390740355567705E62d,
            +2.5526681615684215E63d,
            +6.938871462941557E63d,
            +1.8861808782043154E64d,
            +5.1271712215233855E64d,
            +1.3937096689052236E65d,
            +3.7884955399150257E65d,
            +1.0298199046367501E66d,
            +2.799340708992666E66d,
            +7.609396391563323E66d,
            +2.0684484008569103E67d,
            +5.622626080395226E67d,
            +1.528388084444653E68d,
            +4.1545899609113734E68d,
            +1.1293346659459732E69d,
            +3.069849599753188E69d,
            +8.344717266683004E69d,
            +2.268329019570017E70d,
            +6.165958325782564E70d,
            +1.676081191364984E71d,
            +4.556060380835955E71d,
            +1.2384658100355657E72d,
            +3.3664990715562672E72d,
            +9.15109220707761E72d,
            +2.4875248571153216E73d,
            +6.761793219649385E73d,
            +1.8380461271305958E74d,
            +4.996327312938759E74d,
            +1.3581426848077408E75d,
            +3.691814001080034E75d,
            +1.0035391101975138E76d,
            +2.7279024753382288E76d,
            +7.415207287657125E76d,
            +2.0156621983963848E77d,
            +5.479138512760614E77d,
            +1.4893842728520671E78d,
            +4.048565732162643E78d,
            +1.1005142643914475E79d,
            +2.991508131437659E79d,
            +8.131762373533769E79d,
            +2.210442148596269E80d,
            +6.008604166110734E80d,
            +1.633308028614055E81d,
            +4.439791652732591E81d,
            +1.206860599814453E82d,
            +3.280586734644871E82d,
            +8.917559854082513E82d,
            +2.4240442814945802E83d,
            +6.589235682116406E83d,
            +1.7911398904871E84d,
            +4.86882298924053E84d,
            +1.3234832005748183E85d,
            +3.597600556519039E85d,
            +9.77929222446451E85d,
            +2.658286976862848E86d,
            +7.225974166887662E86d,
            +1.9642232209552433E87d,
            +5.3393125705958075E87d,
            +1.4513757076459615E88d,
            +3.945247871835613E88d,
            +1.0724295693252266E89d,
            +2.915165904253785E89d,
            +7.924242330665303E89d,
            +2.1540322390343345E90d,
            +5.855267177907345E90d,
            +1.5916266807316476E91d,
            +4.326489915443873E91d,
            +1.1760619079592718E92d,
            +3.1968677404735245E92d,
            +8.689987517871135E92d,
            +2.3621834216830225E93d,
            +6.421080550439423E93d,
            +1.7454306955949023E94d,
            +4.744571892885607E94d,
            +1.2897084285532175E95d,
            +3.505791114318544E95d,
            +9.529727908157224E95d,
            +2.5904487437231458E96d,
            +7.041568925985714E96d,
            +1.9140971884979424E97d,
            +5.203055142575272E97d,
            +1.4143368931719686E98d,
            +3.8445667684706366E98d,
            +1.0450615121235744E99d,
            +2.8407720200442806E99d,
            +7.722018663521402E99d,
            +2.0990624115923312E100d,
            +5.705842978547001E100d,
            +1.5510089388648915E101d,
            +4.216079296087462E101d,
            +1.1460491592124923E102d,
            +3.1152847602082673E102d,
            +8.468222063292654E102d,
            +2.3019011105282883E103d,
            +6.257216813084462E103d,
            +1.7008878437355237E104d,
            +4.62349260394851E104d,
            +1.2567956334920216E105d,
            +3.416324322370112E105d,
            +9.286532888251822E105d,
            +2.5243410574836706E106d,
            +6.861870970598542E106d,
            +1.8652499723625443E107d,
            +5.070274654122399E107d,
            +1.3782437251846782E108d,
            +3.746454626411946E108d,
            +1.0183920005400422E109d,
            +2.768276122845335E109d,
            +7.524954624697075E109d,
            +2.0454950851007314E110d,
            +5.56023190218245E110d,
            +1.511427628805191E111d,
            +4.1084862677372065E111d,
            +1.1168024085164686E112d,
            +3.0357834799588566E112d,
            +8.252116273466952E112d,
            +2.2431576057283144E113d,
            +6.097534318207731E113d,
            +1.65748157925005E114d,
            +4.5055022172222453E114d,
            +1.2247224482958058E115d,
            +3.329140840363789E115d,
            +9.049543313665034E115d,
            +2.4599209935197392E116d,
            +6.686758417135634E116d,
            +1.817649308779104E117d,
            +4.940883275207154E117d,
            +1.3430713954289087E118d,
            +3.6508464654683645E118d,
            +9.924030156169606E118d,
            +2.697631034485758E119d,
            +7.332921137166064E119d,
            +1.9932945470297703E120d,
            +5.418336099279846E120d,
            +1.472856595860236E121d,
            +4.0036393271908754E121d,
            +1.0883019300873278E122d,
            +2.9583112936666607E122d,
            +8.041523923017192E122d,
            +2.1859129781586158E123d,
            +5.941927186144745E123d,
            +1.6151834292371802E124d,
            +4.390523815859274E124d,
            +1.1934680816813702E125d,
            +3.2441826014060764E125d,
            +8.81860282490643E125d,
            +2.3971445233885962E126d,
            +6.516115189736396E126d,
            +1.7712635751001657E127d,
            +4.814793918384117E127d,
            +1.3087966177291396E128d,
            +3.557678449715009E128d,
            +9.670771210463886E128d,
            +2.628788218289742E129d,
            +7.145787619369324E129d,
            +1.9424264981694277E130d,
            +5.280062387569078E130d,
            +1.4352697002457768E131d,
            +3.901467289560222E131d,
            +1.0605288965077546E132d,
            +2.882816299252225E132d,
            +7.836307815186044E132d,
            +2.1301292155181736E133d,
            +5.790291758828013E133d,
            +1.573964437869041E134d,
            +4.278478878300888E134d,
            +1.1630112062985817E135d,
            +3.1613917467297413E135d,
            +8.593554223894477E135d,
            +2.335970335559215E136d,
            +6.349826172787151E136d,
            +1.7260616357651607E137d,
            +4.691921416188566E137d,
            +1.2753966504932798E138d,
            +3.466887271843006E138d,
            +9.423976538577447E138d,
            +2.561702766944378E139d,
            +6.963429563637273E139d,
            +1.892856346657855E140d,
            +5.1453167686439515E140d,
            +1.3986421289359558E141d,
            +3.8019036618832785E141d,
            +1.033464507572145E142d,
            +2.809247950589945E142d,
            +7.636326960498012E142d,
            +2.075769060297565E143d,
            +5.64252553828769E143d,
            +1.5337974510118784E144d,
            +4.169293918423203E144d,
            +1.1333315586787883E145d,
            +3.080714152600695E145d,
            +8.374250298636991E145d,
            +2.276357074042286E146d,
            +6.187780443461367E146d,
            +1.6820131331794073E147d,
            +4.572185635487065E147d,
            +1.2428488853188662E148d,
            +3.378413594504258E148d,
            +9.183480622172801E148d,
            +2.4963286658278886E149d,
            +6.785725312893433E149d,
            +1.8445514681108982E150d,
            +5.014010481958507E150d,
            +1.3629491735708616E151d,
            +3.7048805655699485E151d,
            +1.0070909418550386E152d,
            +2.7375567044077912E152d,
            +7.441451374243517E152d,
            +2.022795961737854E153d,
            +5.4985298195094216E153d,
            +1.494655405262451E154d,
            +4.062894701808608E154d,
            +1.1044092571980793E155d,
            +3.002095574584687E155d,
            +8.160542326793782E155d,
            +2.218265110516721E156d,
            +6.02987028472758E156d,
            +1.6390888071605646E157d,
            +4.455504920700703E157d,
            +1.2111317421229415E158d,
            +3.2921976772303727E158d,
            +8.94912101169977E158d,
            +2.432623425087251E159d,
            +6.612555731556604E159d,
            +1.7974788874847574E160d,
            +4.8860545948985793E160d,
            +1.328167263606087E161d,
            +3.610333312791256E161d,
            +9.813901863427107E161d,
            +2.667695552814763E162d,
            +7.251548346906463E162d,
            +1.9711751621240536E163d,
            +5.3582093498119173E163d,
            +1.4565123573071036E164d,
            +3.959211091077107E164d,
            +1.0762251933089556E165d,
            +2.9254832789181E165d,
            +7.952287052787358E165d,
            +2.161656025361765E166d,
            +5.8759898326913254E166d,
            +1.597259768214821E167d,
            +4.3418021646459346E167d,
            +1.1802241249113175E168d,
            +3.2081817253680657E168d,
            +8.720743087611513E168d,
            +2.3705435424427623E169d,
            +6.443805025317327E169d,
            +1.7516078165936552E170d,
            +4.7613641572445654E170d,
            +1.2942728582966776E171d,
            +3.518198614137319E171d,
            +9.563454814394247E171d,
            +2.5996166206245285E172d,
            +7.066491077377918E172d,
            +1.920871394985668E173d,
            +5.221469250951617E173d,
            +1.4193426880442385E174d,
            +3.8581732071331E174d,
            +1.0487601931965087E175d,
            +2.850825930161946E175d,
            +7.749348772180658E175d,
            +2.1064911705560668E176d,
            +5.726036941135634E176d,
            +1.5564982816556894E177d,
            +4.231000988846797E177d,
            +1.1501053030837989E178d,
            +3.1263099916916113E178d,
            +8.498192212235393E178d,
            +2.3100480183046895E179d,
            +6.279361500971995E179d,
            +1.7069074829463731E180d,
            +4.63985600437427E180d,
            +1.2612435745231905E181d,
            +3.4284156709489884E181d,
            +9.319400030019162E181d,
            +2.5332752658571312E182d,
            +6.88615578404537E182d,
            +1.8718514371423056E183d,
            +5.088219872370737E183d,
            +1.3831214731781958E184d,
            +3.759713966511158E184d,
            +1.021996184153141E185d,
            +2.778073442169904E185d,
            +7.55158797540476E185d,
            +2.0527342305586606E186d,
            +5.579910641313343E186d,
            +1.5167767828844167E187d,
            +4.123026721295484E187d,
            +1.1207549425651513E188d,
            +3.0465278560980536E188d,
            +8.281321669236493E188d,
            +2.251096660331649E189d,
            +6.119114404399683E189d,
            +1.6633478556884994E190d,
            +4.521448560089285E190d,
            +1.2290570545894685E191d,
            +3.340923580982338E191d,
            +9.081571104550255E191d,
            +2.468626868232408E192d,
            +6.710424255583952E192d,
            +1.8240823171621646E193d,
            +4.958369974640573E193d,
            +1.3478247120462365E194d,
            +3.6637673548790206E194d,
            +9.959152908532152E194d,
            +2.707178052117959E195d,
            +7.358873642076596E195d,
            +2.0003490682463053E196d,
            +5.4375131636754E196d,
            +1.4780692924846082E197d,
            +4.01780853635105E197d,
            +1.0921536132159379E198d,
            +2.968781250496917E198d,
            +8.069984512111955E198d,
            +2.193649279840519E199d,
            +5.962956589227457E199d,
            +1.620899738203635E200d,
            +4.406062052965071E200d,
            +1.1976919074588434E201d,
            +3.2556641859513496E201d,
            +8.849812639395597E201d,
            +2.40562867677584E202d,
            +6.539175932653188E202d,
            +1.7775323307944624E203d,
            +4.831833881898182E203d,
            +1.3134287685114547E204d,
            +3.5702693195009266E204d,
            +9.704997606668411E204d,
            +2.63809219778715E205d,
            +7.171077244202293E205d,
            +1.949300880034352E206d,
            +5.298749302736127E206d,
            +1.4403494631058154E207d,
            +3.91527572177694E207d,
            +1.0642823992403076E208d,
            +2.8930193727937684E208d,
            +7.8640411896421955E208d,
            +2.1376680994038112E209d,
            +5.8107841809216616E209d,
            +1.5795351101531684E210d,
            +4.293620869258453E210d,
            +1.1671272667059652E211d,
            +3.172580666390786E211d,
            +8.623968972387222E211d,
            +2.3442378838418366E212d,
            +6.372298757235201E212d,
            +1.7321703934464356E213d,
            +4.708527306855985E213d,
            +1.279910496643312E214d,
            +3.479157135998568E214d,
            +9.45732984079136E214d,
            +2.5707689593428096E215d,
            +6.988074107282322E215d,
            +1.8995553996578656E216d,
            +5.1635269305465607E216d,
            +1.4035923083915864E217d,
            +3.815359096108819E217d,
            +1.0371220592190472E218d,
            +2.819190456167585E218d,
            +7.663353127378024E218d,
            +2.083115484919861E219d,
            +5.662495731848751E219d,
            +1.5392257142577226E220d,
            +4.184049381430498E220d,
            +1.1373425785132867E221d,
            +3.091617462831603E221d,
            +8.403887374207366E221d,
            +2.2844135610697528E222d,
            +6.209679892802781E222d,
            +1.6879660933816274E223d,
            +4.588367423411997E223d,
            +1.2472476068464461E224d,
            +3.3903703993793316E224d,
            +9.215982463319503E224d,
            +2.5051637206758385E225d,
            +6.809741127603255E225d,
            +1.8510795864289367E226d,
            +5.031755776868959E226d,
            +1.3677729802316034E227d,
            +3.7179924024793253E227d,
            +1.0106552237522032E228d,
            +2.7472456017809066E228d,
            +7.467788172398272E228d,
            +2.029955237703202E229d,
            +5.517990469846618E229d,
            +1.4999452522236406E230d,
            +4.0772734783595525E230d,
            +1.1083180046837618E231d,
            +3.012720614547867E231d,
            +8.18942426109357E231d,
            +2.2261161215322043E232d,
            +6.051211457626543E232d,
            +1.6448897917725177E233d,
            +4.471273900208441E233d,
            +1.2154183152078517E234d,
            +3.3038494682728794E234d,
            +8.98079409878202E234d,
            +2.4412328161430576E235d,
            +6.63595840453991E235d,
            +1.8038406914061554E236d,
            +4.90334700062756E236d,
            +1.3328680266667662E237d,
            +3.623110695743118E237d,
            +9.848636053777669E237d,
            +2.677136737066629E238d,
            +7.277212447141125E238d,
            +1.978151484427976E239d,
            +5.377173488599035E239d,
            +1.4616672175682191E240d,
            +3.973222981713661E240d,
            +1.0800340064859439E241d,
            +2.935837009891444E241d,
            +7.980432566722885E241d,
            +2.169306470354036E242d,
            +5.896786161387733E242d,
            +1.6029126916635028E243d,
            +4.357168123448786E243d,
            +1.1844011798406507E244d,
            +3.2195361624179725E244d,
            +8.751606149833694E244d,
            +2.3789334438756013E245d,
            +6.466611224443739E245d,
            +1.7578073785142153E246d,
            +4.7782149589194885E246d,
            +1.2988535295611824E247d,
            +3.5306502960727705E247d,
            +9.597302512507479E247d,
            +2.608817438130718E248d,
            +7.091500562953208E248d,
            +1.9276698418065647E249d,
            +5.239949786641934E249d,
            +1.42436589329759E250d,
            +3.8718282216768776E250d,
            +1.0524719896550007E251d,
            +2.860915548426704E251d,
            +7.77677492833005E251d,
            +2.113946677051906E252d,
            +5.7463023795153145E252d,
            +1.56200679236425E253d,
            +4.2459748085663055E253d,
            +1.1541756557557508E254d,
            +3.137374584307575E254d,
            +8.528268445871411E254d,
            +2.3182239583484444E255d,
            +6.301585387776819E255d,
            +1.7129486892266285E256d,
            +4.6562769567905925E256d,
            +1.26570724146049E257d,
            +3.4405490416979487E257d,
            +9.352382323649647E257d,
            +2.54224113415832E258d,
            +6.910528108396216E258d,
            +1.8784760208391767E259d,
            +5.106228040084293E259d,
            +1.3880166914480165E260d,
            +3.7730204737910044E260d,
            +1.0256131352582533E261d,
            +2.787906051540986E261d,
            +7.578313650939932E261d,
            +2.0599991793068063E262d,
            +5.5996586041611455E262d,
            +1.522145133131402E263d,
            +4.137618951061827E263d,
            +1.1247213964487372E264d,
            +3.0573102223682595E264d,
            +8.310629417537063E264d,
            +2.2590636576955473E265d,
            +6.1407711078356886E265d,
            +1.6692346202615142E266d,
            +4.5374504961394207E266d,
            +1.2334070098307164E267d,
            +3.3527476928456816E267d,
            +9.113713162029408E267d,
            +2.4773638527240193E268d,
            +6.734172833429278E268d,
            +1.8305382378470305E269d,
            +4.9759187284770303E269d,
            +1.352594940263854E270d,
            +3.6767339705169146E270d,
            +9.994400500679653E270d,
            +2.716759624268743E271d,
            +7.384918458508588E271d,
            +2.007428933605617E272d,
            +5.456757565532369E272d,
            +1.4833003969415539E273d,
            +4.0320284712983994E273d,
            +1.096019026243815E274d,
            +2.979288529962515E274d,
            +8.098545495417704E274d,
            +2.201412886580694E275d,
            +5.984060832462728E275d,
            +1.6266362950862408E276d,
            +4.4216561713555547E276d,
            +1.2019307065458128E277d,
            +3.2671863888979078E277d,
            +8.881133159512924E277d,
            +2.4141423627760256E278d,
            +6.562319473965767E278d,
            +1.7838233889223407E279d,
            +4.848934634563382E279d,
            +1.3180771991576186E280d,
            +3.5829049382293792E280d,
            +9.739345931419228E280d,
            +2.6474285478041252E281d,
            +7.196457718729758E281d,
            +1.956199868121249E282d,
            +5.31750271790054E282d,
            +1.4454470027638629E283d,
            +3.929132560365955E283d,
            +1.0680488848057261E284d,
            +2.9032581477488686E284d,
            +7.89187408872514E284d,
            +2.1452336456259667E285d,
            +5.831349876080173E285d,
            +1.5851251724785243E286d,
            +4.308816643345461E286d,
            +1.1712579802975851E287d,
            +3.1838092090922606E287d,
            +8.654490685278886E287d,
            +2.3525345191912968E288d,
            +6.39485115791896E288d,
            +1.7383009254496851E289d,
            +4.725191397657393E289d,
            +1.2844402232816276E290d,
            +3.491470347090126E290d,
            +9.490800658395667E290d,
            +2.579867270991543E291d,
            +7.012806239173502E291d,
            +1.906278351789277E292d,
            +5.181801397059486E292d,
            +1.408559707497606E293d,
            +3.8288623079292884E293d,
            +1.0407926842436056E294d,
            +2.829168201470791E294d,
            +7.690475570840264E294d,
            +2.0904882610105383E295d,
            +5.68253547942899E295d,
            +1.544673396032028E296d,
            +4.1988574190754736E296d,
            +1.1413677466646359E297d,
            +3.102559332875688E297d,
            +8.433630296371073E297d,
            +2.292498520423419E298d,
            +6.23165710486722E298d,
            +1.6939399242810123E299d,
            +4.604606371472047E299d,
            +1.2516618713553432E300d,
            +3.402369329874797E300d,
            +9.248598815279678E300d,
            +2.51402968559859E301d,
            +6.833842035076675E301d,
            +1.8576309291617257E302d,
            +5.049564425991982E302d,
            +1.3726137091534984E303d,
            +3.7311513682845094E303d,
            +1.0142320772726397E304d,
            +2.7569686255975333E304d,
            +7.494218049456063E304d,
            +2.037139607241041E305d,
            +5.5375196488302575E305d,
            +1.5052539519895093E306d,
            +4.091704288360009E306d,
            +1.1122405335641184E307d,
            +3.023383151402969E307d,
            +8.218407798110846E307d,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
    };

    private static final double[] EXP_INT_TABLE_B = new double[]{
            +0.0d,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            -1.76097684E-316d,
            -2.44242319E-315d,
            -9.879317845E-315d,
            -1.3811462167E-314d,
            +2.1775261204E-314d,
            -1.4379095864E-313d,
            +1.4219324087E-313d,
            +1.00605438061E-312d,
            -1.287101187097E-312d,
            +5.33839690397E-312d,
            -9.35130825405E-313d,
            -4.15218681073E-311d,
            +4.546040329134E-311d,
            -1.57333572310673E-310d,
            +1.05387548454467E-309d,
            +2.095732474644446E-309d,
            -2.62524392470767E-310d,
            +5.86440876259637E-309d,
            -2.401816502004675E-309d,
            -2.2711230715729753E-308d,
            +2.0670460065057715E-307d,
            +3.436860020483706E-308d,
            +2.0862243734177337E-306d,
            -4.637025318037353E-306d,
            +9.222671009756424E-306d,
            +6.704597874020559E-305d,
            +4.351284159444109E-305d,
            +4.232889602759328E-304d,
            +1.2840977763293412E-303d,
            -2.6993478083348727E-303d,
            -1.053265874779237E-303d,
            +1.207746682843556E-303d,
            +5.21281096513035E-303d,
            +1.6515377082609677E-301d,
            +3.3951607353932444E-301d,
            +5.609418227003629E-301d,
            +4.238775357914848E-300d,
            -9.441842771290538E-300d,
            -2.1745347282493023E-299d,
            -6.203839803215248E-299d,
            -5.617718879466363E-299d,
            +5.2869976233132615E-298d,
            -1.4300075619643524E-298d,
            +4.3198234936686506E-297d,
            -2.6448316331572387E-297d,
            +4.315655444002347E-296d,
            -7.253671992213344E-296d,
            -1.1288398461391523E-295d,
            -4.83901764243093E-296d,
            +1.7407497662694827E-295d,
            +1.1969717029666017E-294d,
            -7.752519943329177E-294d,
            -4.019569741253664E-293d,
            -2.4467928392518484E-293d,
            -1.0269233640424235E-292d,
            -3.2330960700986594E-292d,
            -1.440995270758115E-291d,
            -3.726946038150935E-291d,
            -1.3424576100819801E-291d,
            -3.128894928199484E-290d,
            -5.989337506920005E-290d,
            -9.438168176533759E-290d,
            -1.9220613500411237E-289d,
            +2.1186736024949195E-289d,
            +6.3015208029537436E-288d,
            -8.168129112703755E-288d,
            -1.6040513288090055E-287d,
            -1.0809972724404233E-287d,
            -3.080380385962424E-286d,
            +2.6399157174374624E-286d,
            +1.3317127674213423E-285d,
            -3.5821668044872306E-285d,
            +1.978536584535392E-284d,
            +1.3399392455370071E-284d,
            -2.870168560029448E-284d,
            +3.5311184272663063E-283d,
            -7.204247881190918E-283d,
            +3.2425604548983798E-282d,
            +3.913063150326019E-282d,
            -2.260957518848075E-281d,
            +3.807242187736102E-281d,
            -5.095591405025083E-281d,
            +2.3400625068490396E-280d,
            -1.1564717694090882E-280d,
            -3.517594695450786E-279d,
            +6.666544384808297E-279d,
            -9.204784113858607E-279d,
            +4.8677119923665573E-278d,
            +7.942176091555472E-278d,
            -2.5113270522478854E-277d,
            +5.332900939354667E-277d,
            -3.491241408725929E-276d,
            -2.1141094074221325E-276d,
            +1.722049095222509E-275d,
            +4.0430160253378594E-275d,
            +1.9888195459082551E-274d,
            +3.230089643550739E-275d,
            +5.077824728028163E-274d,
            -3.526547961682877E-274d,
            -6.4376298274983765E-273d,
            -2.5338279333399964E-272d,
            -3.614847626733713E-272d,
            +2.510812179067931E-272d,
            +3.953806005373127E-272d,
            +7.112596406315374E-272d,
            -2.850217520533226E-270d,
            -8.571477929711754E-270d,
            +1.2902019831221148E-269d,
            -6.978783784755863E-270d,
            +9.89845486618531E-269d,
            -3.538563171970534E-268d,
            +3.537475449241181E-268d,
            +3.6924578046381256E-267d,
            +1.3555502536444713E-266d,
            -1.1279742372661484E-266d,
            +5.475072932318336E-266d,
            -1.1679889049814275E-265d,
            -8.946297908979776E-266d,
            +1.0565816011650582E-264d,
            -3.2161237736296753E-265d,
            -6.022045553485609E-264d,
            -2.0332050860436034E-263d,
            -1.0488538406930105E-262d,
            +1.6793752843984384E-262d,
            +3.2558720916543104E-263d,
            -1.9546569053899882E-262d,
            +5.082190670014963E-262d,
            -1.0188117475357564E-260d,
            +3.7920054509691455E-261d,
            -8.330969967504819E-260d,
            -1.1623181434592597E-259d,
            +9.09665088462258E-259d,
            -1.56400149127482E-259d,
            -7.796557225750673E-258d,
            +6.751460509863465E-258d,
            +7.243157658226935E-258d,
            +1.2574668958946027E-256d,
            +2.2678858131411216E-256d,
            +5.1079306249351287E-256d,
            -5.672261759108003E-257d,
            +3.476539491009769E-256d,
            -1.3481093992496937E-254d,
            -3.314051560952014E-254d,
            +7.408112967339146E-255d,
            -7.164884605413269E-254d,
            -6.456588023278983E-253d,
            -1.4881197370811587E-252d,
            +1.7534012237555307E-252d,
            -1.3070101381473173E-251d,
            +6.081420141954215E-251d,
            +6.591143677421159E-251d,
            +2.6917461073773043E-250d,
            +3.683043641790553E-251d,
            +1.2195076420741757E-249d,
            -8.220283439582378E-249d,
            +1.637852737426943E-248d,
            -8.332543237340988E-249d,
            +2.9581193516975647E-248d,
            -1.7790661150204172E-247d,
            -1.7809679916043692E-247d,
            +8.378574405736031E-247d,
            -2.883847036065813E-246d,
            +1.3223776943337897E-245d,
            +3.098547586845664E-245d,
            -1.1036542789147287E-244d,
            -5.7187703271582225E-244d,
            -1.8058492822440396E-244d,
            +4.4373726292703545E-243d,
            -3.4631935816990754E-243d,
            -1.82770041073856E-243d,
            +3.845535085273936E-242d,
            +8.446532344375812E-242d,
            +2.7751016140238277E-242d,
            +1.3158882241538003E-241d,
            -3.579433051074272E-240d,
            -6.151751570213211E-240d,
            -2.990535475079021E-239d,
            +2.3396028616528764E-239d,
            +7.233790684263346E-239d,
            +1.0847913100494912E-238d,
            +7.103148400942551E-238d,
            +3.463600299750966E-237d,
            -4.873121855093712E-237d,
            +1.3407295326570417E-236d,
            +9.390271617387205E-237d,
            -2.4767709454727603E-235d,
            +3.205923535388443E-235d,
            -1.0074984709952582E-234d,
            +2.4747880175747574E-234d,
            -5.146939682310558E-234d,
            -2.827581009333298E-233d,
            -3.0307641004671077E-233d,
            +5.92044714050651E-233d,
            -2.0582596893119236E-232d,
            -6.58066591313112E-232d,
            -4.869955151949929E-231d,
            -5.763495903609913E-231d,
            -2.3580462372762525E-230d,
            +1.8559980428862584E-230d,
            +2.854978560542175E-229d,
            +5.637945686485334E-229d,
            +2.1454644909004582E-228d,
            -1.1918070206953359E-228d,
            -5.021851606912854E-228d,
            +3.861525553653117E-227d,
            +6.533561982617909E-227d,
            -3.015709444206057E-226d,
            -5.042005018212734E-227d,
            +1.5959614205422845E-225d,
            +2.0402105689098835E-224d,
            +5.164902728917601E-224d,
            +9.981031744879876E-224d,
            +4.0281104210095145E-223d,
            +1.1158160971176672E-222d,
            +2.0736172194624895E-222d,
            +4.983162653734032E-222d,
            +2.1753390051977871E-221d,
            +3.969413618002761E-221d,
            +1.3961255018698695E-220d,
            +2.1290855095314206E-220d,
            +1.1927747883417406E-219d,
            +3.7264401117998796E-219d,
            +9.318532410862293E-219d,
            +2.3414841777613345E-218d,
            +4.3791842770430786E-218d,
            +1.7173159016511951E-217d,
            +3.5037536832675478E-217d,
            +1.4300098613455884E-216d,
            +2.4189403362149483E-216d,
            +9.306541421999056E-216d,
            +3.442100456607687E-215d,
            +5.94407068841904E-215d,
            +2.0483260435783403E-214d,
            +3.8410992889527954E-214d,
            +1.2038281262953917E-213d,
            +3.865007795216205E-213d,
            +9.754659138599756E-213d,
            +2.7653605770745684E-212d,
            +5.359568079675375E-212d,
            +2.61726605666378E-211d,
            +5.054202073556894E-211d,
            +8.707092668016246E-211d,
            +1.4080573899148006E-210d,
            +1.288124387778789E-209d,
            +1.8639901642011898E-209d,
            +6.076014540574561E-209d,
            +1.798489141298457E-208d,
            +2.1525406805994896E-208d,
            +1.1864056832305874E-207d,
            +2.1077440662171152E-207d,
            +1.3784853708457332E-206d,
            +1.6965806532093783E-206d,
            +7.241626420445137E-206d,
            +2.575584299085016E-205d,
            +6.151951078101721E-205d,
            +2.40652042118887E-204d,
            +4.022633486003565E-204d,
            +5.8840879519086286E-204d,
            +3.2820308007277566E-203d,
            +4.31880454864738E-203d,
            +2.427240455243201E-202d,
            +7.326955749884755E-202d,
            +1.4310184489676175E-201d,
            +4.464279133463661E-201d,
            +4.895131474682867E-201d,
            +4.48614966943544E-200d,
            +8.924048768324976E-200d,
            +2.5035535029701945E-199d,
            +6.627829836338812E-199d,
            +2.6066826304502746E-198d,
            +8.042275310036546E-198d,
            +2.115062964308555E-197d,
            +4.413745413236018E-197d,
            +1.644449394585716E-196d,
            +3.138217752973845E-196d,
            +7.48533983136081E-196d,
            +2.613626422028823E-195d,
            +3.6741841454219095E-195d,
            +5.906102862953403E-195d,
            +4.4940857547850743E-194d,
            +5.840064709376958E-194d,
            +3.087661273836024E-193d,
            +4.995552216100365E-193d,
            +1.991444798915497E-192d,
            +7.097454751809522E-192d,
            +2.0510193986749737E-191d,
            +5.759440286608551E-191d,
            +1.7013941257113314E-190d,
            +2.1383323934483528E-190d,
            +8.280292810015406E-190d,
            +3.138655772049104E-189d,
            +7.961506427685701E-189d,
            +2.0579001228504997E-188d,
            +7.530840351477639E-188d,
            +1.4582863136475673E-187d,
            +3.149267215638608E-187d,
            +5.443114553057336E-187d,
            +3.4672966834277804E-186d,
            +7.374944406615125E-186d,
            +2.7318417252599104E-185d,
            +7.913674211949961E-185d,
            +2.5217716516462005E-184d,
            +4.0866585874353075E-184d,
            +1.2087698972768686E-183d,
            +3.7072473866919033E-183d,
            +1.1333588840402273E-182d,
            +1.61949812578045E-182d,
            +6.567779607147072E-182d,
            +2.422974840736314E-181d,
            +2.551170809294396E-181d,
            +1.0905890688083124E-180d,
            +3.221279639653057E-180d,
            +7.068244813489027E-180d,
            +1.3752309224575428E-179d,
            +7.20154303462761E-179d,
            +1.5391707185581056E-178d,
            +7.708777608683431E-178d,
            +5.597398155472547E-178d,
            +1.8487854656676722E-177d,
            +1.0577249492414076E-176d,
            +2.8926683313922764E-176d,
            +4.090184282164232E-176d,
            +1.6142943398013813E-175d,
            +7.873864351702525E-175d,
            +2.242630017261011E-174d,
            +3.4637009373878283E-174d,
            +1.5907089565090164E-173d,
            +1.6985075903314236E-173d,
            +1.1552273904608563E-172d,
            +2.237894048535414E-172d,
            +5.321990399912051E-172d,
            +1.4106062639738257E-171d,
            +2.9850404523368767E-171d,
            +1.5683802588004895E-170d,
            +4.880146806045633E-170d,
            +1.1489352403441815E-169d,
            +1.6401586605693734E-169d,
            +8.29169700697816E-169d,
            +1.0380723705441457E-168d,
            +7.126414081261746E-168d,
            +1.253325949455206E-167d,
            +2.595079206183114E-167d,
            +1.537490712803659E-166d,
            +2.6338455225993276E-166d,
            +7.994936425058567E-166d,
            +1.5716634677516025E-165d,
            +3.669404761339415E-165d,
            +1.9941628263579332E-164d,
            +4.5012079983352374E-164d,
            +7.283163019991001E-164d,
            +2.398038505188316E-163d,
            +7.868666894503849E-163d,
            +2.1478649410390003E-162d,
            +8.306111510463843E-162d,
            +1.5453160659068463E-161d,
            -4.590496588813841E-162d,
            +3.5449293983801232E-161d,
            -1.0440854056870505E-160d,
            -2.321064927632431E-160d,
            +5.707867001443433E-160d,
            -2.238614484037969E-159d,
            +2.482282821883242E-159d,
            -1.1508772192025259E-158d,
            +1.9903990578876104E-158d,
            -1.2116165315442256E-158d,
            -2.9084557554502667E-157d,
            -1.1211083853006645E-156d,
            -1.309893394818129E-156d,
            +4.2269712317468864E-156d,
            -7.678973146281339E-156d,
            +3.2021376921211934E-155d,
            -7.08313012515209E-155d,
            +1.944398214330544E-154d,
            +1.1860061363751161E-153d,
            +1.5234465914578058E-153d,
            -2.9020908354550263E-153d,
            +4.980100072851796E-153d,
            +2.3101551448625578E-152d,
            -1.1959241322537072E-151d,
            -9.27398924154364E-153d,
            +5.999390491704392E-152d,
            +1.3373196561281372E-150d,
            -1.0271780540759147E-150d,
            +2.575620466387945E-150d,
            -6.56250013356227E-149d,
            -1.1961357917482867E-148d,
            +5.5807813570926636E-148d,
            +9.252840189663807E-148d,
            -1.830335419852293E-147d,
            +9.350990339947455E-147d,
            -1.6072409939877762E-146d,
            -2.5309995887229526E-146d,
            -1.6014373376410622E-146d,
            -3.303297758377758E-145d,
            +1.5640419864850202E-145d,
            +9.544642884951585E-145d,
            -8.64864445321803E-144d,
            +7.580392204597681E-144d,
            +2.678334184447568E-143d,
            -3.7269289985326055E-143d,
            -2.851965258161176E-142d,
            +7.243267286265823E-142d,
            +4.4510805312036926E-141d,
            +9.008499734799015E-141d,
            +1.130435759928337E-140d,
            -3.096539751496479E-140d,
            -1.497405487919762E-139d,
            +3.51519845948652E-139d,
            -4.713790209541894E-139d,
            +4.740753295616865E-138d,
            +9.517570994930463E-138d,
            -1.8842098029339485E-137d,
            -3.825558165008403E-137d,
            +1.1817638600528107E-136d,
            -3.514601201473235E-136d,
            -6.344612631552417E-136d,
            -1.6754164785291923E-136d,
            +4.445372986583078E-135d,
            -3.89604237755475E-134d,
            -1.0155552195374609E-134d,
            +2.1858142063550155E-134d,
            +3.497714990137842E-133d,
            -7.635830383612894E-133d,
            +1.2050744860079718E-132d,
            -7.683019590615251E-133d,
            -3.344806129021162E-131d,
            -1.6737914131474577E-131d,
            -4.30610076666344E-131d,
            +5.184023388254773E-130d,
            +2.6290763595212492E-129d,
            +7.90041744728452E-130d,
            -3.204473056113754E-129d,
            -2.552517201762272E-128d,
            +7.130134251490065E-128d,
            -3.2244113258340395E-127d,
            -1.064920993515727E-127d,
            +2.7466520735457463E-126d,
            +4.368312797746065E-126d,
            +1.8802599072446818E-125d,
            -4.257625799463564E-125d,
            +5.491672256552995E-125d,
            +3.7298611779671127E-124d,
            +5.724180836308973E-124d,
            +1.3861841053630075E-123d,
            +4.2303826056297614E-123d,
            +3.5335436928899096E-123d,
            -2.522906629540626E-122d,
            +1.0147808005267102E-121d,
            +6.734406065735473E-122d,
            -4.948973160958133E-121d,
            +2.4256181927024344E-120d,
            +4.9056283164780554E-120d,
            +6.846440394397547E-120d,
            +3.512747689569002E-119d,
            -9.020907406701404E-119d,
            +2.5718749916003624E-118d,
            +4.3724191002977524E-119d,
            +1.001455050575191E-117d,
            -2.4442443105031435E-117d,
            +2.38873950760028E-116d,
            -4.831068747037129E-118d,
            -5.148989321866988E-116d,
            +1.7875271881514469E-115d,
            -1.1821586412088555E-114d,
            +4.43247726423679E-115d,
            +4.634817120492781E-114d,
            +1.671311907037975E-113d,
            -4.595250028278979E-113d,
            -5.905511605694905E-113d,
            -1.3657642265608213E-112d,
            +2.881416869529271E-112d,
            +2.1253302469985373E-111d,
            -5.301386276260592E-111d,
            +1.4198782892306878E-112d,
            -3.395494928605007E-110d,
            +9.284633292147283E-110d,
            -6.054133004896379E-110d,
            -8.324100783330331E-109d,
            -2.193190669794277E-108d,
            +1.3613655394659198E-107d,
            +6.463452607647978E-108d,
            +1.0187183636134904E-106d,
            +1.0705673935006142E-106d,
            +2.509050608571019E-106d,
            -1.5096182622106617E-105d,
            +1.7794190449526737E-106d,
            +1.2261246749706581E-104d,
            +2.1377905661197194E-104d,
            -2.2015877944429946E-104d,
            +7.873970951802825E-104d,
            -1.7999197335480384E-103d,
            +1.0487383011058756E-105d,
            -2.9988278531841556E-102d,
            +4.7976477743232285E-102d,
            +3.452316818502442E-102d,
            +5.89953246760617E-101d,
            -4.0785601577267006E-101d,
            +2.7214076662438963E-100d,
            +5.237807655758373E-100d,
            +6.180972117932364E-99d,
            -1.3019742873005683E-98d,
            +4.501188264957416E-99d,
            -2.4075054705261798E-98d,
            +1.6503086546628772E-97d,
            -6.878666975101243E-97d,
            +1.196718116616528E-96d,
            +2.476190162339265E-96d,
            -7.1844969234484515E-96d,
            +5.088386759261555E-95d,
            +6.749368983223726E-95d,
            +1.965737856765605E-94d,
            -5.574080023496771E-94d,
            +1.2493696959436675E-93d,
            +8.533262777516794E-94d,
            -7.225259028588793E-93d,
            -7.340587186324432E-93d,
            -3.482412195764625E-92d,
            +3.4742610108480497E-91d,
            -7.177274244758699E-91d,
            +1.2736636153072213E-90d,
            -5.730160886217076E-90d,
            -1.545495535488274E-89d,
            +1.1304179460367007E-89d,
            +1.249260560756154E-88d,
            -4.7439719382414206E-88d,
            +7.164663249266942E-88d,
            +1.7617425105337704E-87d,
            +2.4175248449172035E-87d,
            -1.043079666926483E-86d,
            -2.8137609614326677E-86d,
            -1.2091497144395591E-85d,
            +3.7944631664558904E-85d,
            -2.8144926807308225E-85d,
            +3.9782728352520784E-85d,
            +4.313978872469646E-84d,
            +5.82190887044604E-84d,
            +5.883385169571802E-83d,
            +1.134857098306787E-82d,
            +3.468049324128309E-82d,
            +2.625423995658143E-82d,
            -3.42827917465521E-81d,
            +5.119461911618321E-81d,
            -2.134387988350615E-80d,
            -4.4703076268400615E-80d,
            +4.806078883451016E-80d,
            +2.3820250362443495E-79d,
            -7.258551497833573E-79d,
            -4.0297835558876335E-78d,
            +2.1424166787650852E-78d,
            -3.2117127164185917E-77d,
            +4.8459153070935316E-77d,
            -1.766924303914492E-76d,
            -2.6921749814579492E-76d,
            -4.1291070428848755E-76d,
            +2.2086994756104319E-75d,
            -7.814146377574201E-75d,
            -1.9589778310104216E-74d,
            +6.52658129486538E-74d,
            +1.7804909509998055E-74d,
            -4.1900132227036916E-73d,
            +1.5705861683841123E-72d,
            -1.904714978998808E-72d,
            -7.81295459930537E-72d,
            +2.818537910881676E-71d,
            +5.840507984320445E-71d,
            +1.7331720051707087E-70d,
            +1.936946987935961E-70d,
            -5.86517231340979E-71d,
            -1.3277440528416646E-69d,
            +1.9906256185827793E-69d,
            +8.668714514280051E-69d,
            +6.643105496829061E-69d,
            -2.5436254170647032E-67d,
            -4.8279217213630774E-67d,
            -1.2640304072937576E-66d,
            +3.51187258511716E-66d,
            +1.4199501303738373E-65d,
            -1.2351697477129173E-65d,
            +7.0542365522414836E-65d,
            +1.030593104122615E-64d,
            -5.452692909894593E-65d,
            -9.415506349675128E-64d,
            -3.6206211446779087E-63d,
            -1.6699188275658641E-62d,
            +2.287280262665656E-62d,
            +7.076135457313529E-62d,
            +2.9019628518165404E-61d,
            -3.1305705497720186E-61d,
            +2.2978757040142953E-60d,
            +1.2424439441817321E-60d,
            +7.140343013236265E-60d,
            +8.633726388939636E-60d,
            +1.3483035574114863E-58d,
            +1.653701058949654E-58d,
            -8.939932297357388E-58d,
            -1.395320103272191E-57d,
            +6.440430933947252E-58d,
            -1.681200826841738E-56d,
            +3.9904382022898837E-56d,
            -4.870518577546228E-56d,
            -1.6990896855901115E-55d,
            -6.751434891261518E-56d,
            -1.669012123121194E-54d,
            -4.079585475491198E-54d,
            -1.3070436427679952E-53d,
            -3.090028378908628E-53d,
            +7.468160889798606E-53d,
            +6.229095980733463E-53d,
            +1.4794751934479566E-52d,
            +1.7444373785853918E-51d,
            -5.3681978363391484E-52d,
            +2.71853394036182E-51d,
            -1.3334367969274016E-50d,
            -1.6958057665854177E-49d,
            -1.452507231312146E-49d,
            +3.3855429446520427E-49d,
            +4.903687986212687E-49d,
            +2.2185957416622524E-48d,
            -9.924196700842429E-48d,
            +4.285128462851149E-47d,
            +3.076063086193525E-48d,
            +4.102052341676543E-46d,
            +1.1745772638457318E-45d,
            -5.309047216809048E-47d,
            +2.72972449891179E-45d,
            -1.1748423022293739E-44d,
            +6.626052626622228E-44d,
            +3.0227439688367925E-44d,
            -4.740494808228372E-43d,
            +5.926057457356852E-43d,
            +3.09768273342776E-42d,
            -5.589493227475577E-42d,
            -8.84908716783327E-42d,
            +2.3684740712822874E-41d,
            +1.4836491430755657E-40d,
            +4.5878801324451396E-40d,
            +1.0585156316103144E-39d,
            +2.3805896467049493E-39d,
            +1.0285082556185196E-38d,
            +2.5187968110874885E-38d,
            -1.4088399542613178E-38d,
            -3.00901028043488E-38d,
            +2.0089026801414973E-37d,
            -1.3324111396289096E-36d,
            +5.458481186294964E-36d,
            -4.8402541351522003E-36d,
            -1.3331969720555312E-35d,
            -8.248332290732976E-35d,
            -1.8349670703969982E-34d,
            +6.403477383195494E-34d,
            +3.7813691654412385E-34d,
            +2.4621305031382827E-33d,
            -5.634051826192439E-33d,
            +3.817173955083142E-32d,
            -6.038239639506472E-32d,
            -2.130447095555397E-31d,
            -6.824454861992054E-31d,
            -1.3455801602048414E-30d,
            -2.518642767561659E-30d,
            +8.082792416221215E-30d,
            +4.718103502869148E-29d,
            -5.607991635038776E-29d,
            -1.8042191582018579E-28d,
            +6.989914264479507E-28d,
            -2.9031739430339586E-28d,
            +6.076820259849921E-27d,
            -3.24981577480893E-27d,
            -2.7648210023059463E-26d,
            -9.785306155980342E-26d,
            +1.241529292737115E-25d,
            +3.0891604448087654E-25d,
            +2.3451052074796954E-24d,
            +6.574128018028633E-24d,
            -1.3345148716925826E-23d,
            +4.3594621428644293E-23d,
            -5.678896695157704E-23d,
            -4.676849004137386E-23d,
            -2.281578975407609E-22d,
            -3.144430608076357E-21d,
            +5.662033727488754E-22d,
            -4.30293375386492E-21d,
            +4.985137671479376E-20d,
            +1.657668502165438E-19d,
            -3.3878706977811337E-19d,
            -7.488022803661722E-19d,
            +1.725039737424264E-18d,
            -6.0275040161173166E-18d,
            -8.081007442213538E-19d,
            +2.9257892371894816E-17d,
            +1.5231541295722552E-16d,
            -1.1474026049124666E-17d,
            +6.890372706231206E-16d,
            +2.592721454922832E-15d,
            -1.1253822296423454E-15d,
            -2.650684279637763E-14d,
            -4.107226967119929E-15d,
            -3.130508064738312E-14d,
            -6.729414275200856E-14d,
            -1.6166170913368169E-12d,
            -1.2059301405584488E-12d,
            -1.2210091619211167E-11d,
            +3.695372823623631E-12d,
            +5.119220484478292E-11d,
            -1.0857572226543142E-10d,
            -4.6490379071586397E-10d,
            -4.5810381714280557E-10d,
            +1.4909756678328582E-9d,
            -1.3155828104004438E-8d,
            -9.149755188170102E-9d,
            +0.0d,
            +8.254840070411029E-8d,
            -1.0681886149151956E-7d,
            -3.359944163407147E-8d,
            -2.1275002921718894E-6d,
            +1.2129920353421116E-5d,
            +2.1520078872608393E-5d,
            +1.0178783359926372E-4d,
            -2.077077172525637E-5d,
            -5.67996159922899E-5d,
            +9.510567165169581E-4d,
            +0.0010901978184553272d,
            +0.010169003920808009d,
            +0.017008920503326107d,
            +0.03416477677774927d,
            -0.1275278893606981d,
            +0.5205078726367633d,
            +0.7535752982147762d,
            +1.1373305111387886d,
            -3.036812739155085d,
            +11.409790277969124d,
            -9.516785302789955d,
            -49.86840843831867d,
            -393.7510973999651d,
            -686.1565277058598d,
            +4617.385872524165d,
            -11563.161235730215d,
            -8230.201383316231d,
            -34460.52482632287d,
            +50744.04207438878d,
            +357908.46214699093d,
            +1936607.425231087d,
            +3222936.695160983d,
            +5477052.0646243105d,
            -3.517545711859706E7d,
            -1.2693418527187027E8d,
            -2.5316384477288628E8d,
            -1.6436423669122624E8d,
            +4.0889180422033095E8d,
            +4.968829330953611E9d,
            -3.503399598592085E9d,
            +1.905394922122271E10d,
            +1.0361722296739479E11d,
            -5.806792575852521E10d,
            +2.3454138776381036E11d,
            -1.718446464587963E12d,
            -1.0946634815588584E12d,
            +1.6889383928999305E13d,
            -3.784600043778247E13d,
            +7.270965670658928E13d,
            -4.9202842786896806E14d,
            +4.597700093952774E14d,
            +2.6113557852262235E15d,
            -4.544525556171388E15d,
            -9.517971970450354E15d,
            -2.0634857819227416E16d,
            -9.7143113104549808E16d,
            -2.2667083759873216E16d,
            -7.2285665164439578E17d,
            +4.1215410760803866E18d,
            +8.5807488300972206E18d,
            +1.530436781375042E19d,
            -1.5453111533064765E19d,
            -1.0633845571643594E20d,
            -3.512380426745336E20d,
            +3.7734658676841284E20d,
            -3.855478664503271E21d,
            +7.984485303520287E21d,
            -1.2296934902142301E22d,
            +1.042139023692827E22d,
            +1.2167897656061312E23d,
            +9.22064170155394E22d,
            +3.965171513035854E23d,
            -4.135121057126514E24d,
            -7.944341754299148E24d,
            +1.4715152230577016E25d,
            -3.0635272288480756E25d,
            -9.54468158713835E25d,
            +1.5411775738825048E25d,
            -8.274711842374368E26d,
            -1.0028324930788433E27d,
            +5.189062091114782E27d,
            -2.8583500869462184E28d,
            -5.198295198128238E28d,
            +2.9758750368256437E29d,
            +3.216046320616945E29d,
            -1.7846700158234043E30d,
            +3.847174961282827E30d,
            +9.026991921214922E30d,
            +4.1358029739592175E30d,
            -6.461509354879894E29d,
            +9.704297297526684E31d,
            +2.9731739067444943E32d,
            +9.97728609663656E32d,
            +3.1149346370027763E33d,
            +2.0051635097366476E34d,
            +2.819272221032373E34d,
            +1.6266731695798413E34d,
            +1.998050894021586E35d,
            -6.1633417615076335E35d,
            +2.2505716077585116E36d,
            +1.9299691540987203E36d,
            +8.006569251375383E36d,
            -3.785295042408568E37d,
            -1.1870498357197593E38d,
            +1.0010529668998112E38d,
            +1.3240710866573994E38d,
            +2.6888010385137123E39d,
            +1.7400655988987023E39d,
            -6.402740469853475E39d,
            -3.93114092562274E40d,
            +1.2363717201084252E41d,
            -1.9219116633978794E41d,
            -1.347867098583136E42d,
            +7.87675118338788E41d,
            +3.3932984011177642E41d,
            -1.9872713979884691E43d,
            +2.220208491349658E43d,
            -3.466267817480825E43d,
            +3.19462030745197E44d,
            -9.841244788104406E44d,
            -2.2676593395522725E45d,
            -1.1349246400274207E46d,
            -1.1700910284427406E46d,
            -3.6754317105801715E46d,
            +1.7647101734915075E47d,
            +2.122358392979746E47d,
            +3.156243682143956E47d,
            +5.356668151937413E47d,
            +2.7668218233914262E48d,
            +3.5127708120698784E48d,
            +1.7884841356632925E49d,
            +1.716531820904728E50d,
            -2.9114757102866277E50d,
            +1.0657703081219677E51d,
            -7.512169809356372E50d,
            +1.764200470879736E51d,
            -1.0088898215431471E52d,
            -3.1085734725176E52d,
            +4.3529009584292495E52d,
            -2.467842129213774E53d,
            -3.9317379627195146E53d,
            -4.332335454045836E52d,
            +7.979013724931926E54d,
            -1.5038413653121357E55d,
            +9.310799925566843E55d,
            -2.2042966348036592E55d,
            -4.518315366841937E55d,
            -6.971366338144781E56d,
            -2.0461505570781806E57d,
            -8.823884392655312E57d,
            -1.1264032993918548E58d,
            -7.692065092509875E58d,
            -1.8472516879728875E59d,
            +8.72220314694275E58d,
            +1.6525336989036362E59d,
            -3.343201925128334E60d,
            +5.493352163155986E60d,
            -2.548073509300398E61d,
            -9.566541624209933E61d,
            +4.0891054447206644E61d,
            -7.724182294653349E62d,
            +1.0143022354947225E63d,
            -4.952031310451961E63d,
            -7.877410133454722E63d,
            +4.505432606253564E64d,
            -7.330635250808021E64d,
            -1.642361029990822E65d,
            +5.982180242124184E65d,
            +7.120242132370469E65d,
            +5.908356249789671E66d,
            -2.8477710945673134E65d,
            +6.65688196961235E66d,
            -9.233295580238604E67d,
            +3.2850043261803593E68d,
            +7.041681569694413E68d,
            -1.5652761725518397E69d,
            +1.5377053215489084E68d,
            +1.282130763903269E70d,
            -2.380286345847567E70d,
            -7.207022875977515E70d,
            +2.7641662602473095E71d,
            +7.685235201534525E71d,
            +4.3239378585884645E70d,
            -1.6840562544109314E72d,
            -5.04128025464686E71d,
            +5.4557485189210095E73d,
            +7.160277784358221E73d,
            +7.636179075087608E73d,
            -8.18804507680012E74d,
            +2.807397988979441E75d,
            +2.165163304600171E75d,
            -1.3208450062862734E76d,
            -5.1939252391404724E76d,
            -6.985952908805853E76d,
            -1.6259920998287064E77d,
            +6.098975200926637E77d,
            -5.63383579957466E77d,
            -1.5876819186852907E78d,
            +2.1487475413123092E79d,
            -3.987619123706934E79d,
            +9.772655251656639E79d,
            -1.638756156057952E79d,
            -7.83892088580041E80d,
            +1.274413296252691E81d,
            +2.51946651720982E81d,
            -2.516866097506943E81d,
            +1.053956282234684E82d,
            +1.8279051206232177E83d,
            +1.2250764591564252E82d,
            -4.0353723442917463E83d,
            -1.4121324224340735E84d,
            -5.45287716696021E84d,
            -1.7514953095665195E85d,
            -5.0706081370522526E85d,
            -4.35799392139009E85d,
            -3.982538093450217E86d,
            -1.4591838284752642E87d,
            +2.5313735821872488E87d,
            -3.718501227185903E86d,
            -1.3907979640327008E88d,
            -5.79002114093961E86d,
            -1.2500675565781447E89d,
            +4.8182788286170926E89d,
            -1.7198866036687559E90d,
            -4.690417668647599E88d,
            +1.3020631859056421E91d,
            -1.3850458263351744E91d,
            +4.87301010703588E91d,
            -1.695546877943826E92d,
            -1.6353756659909833E92d,
            -1.5483926773679628E93d,
            -1.8921091400297595E93d,
            -6.183525570536406E93d,
            -4.987913342551977E93d,
            +1.0186485886120274E93d,
            -1.5343120819745468E95d,
            -5.262123923229857E95d,
            +1.618327917706804E96d,
            -4.135185828158998E96d,
            -8.016793741945299E96d,
            -3.0399439534134115E97d,
            -1.2319346292749103E98d,
            +7.536337311795176E97d,
            -3.577715974851322E98d,
            +2.0521614818695524E99d,
            +1.2627736197958951E98d,
            -5.206910481915062E99d,
            +3.0974593993948837E100d,
            -9.522726334561169E100d,
            -1.1909272509710985E100d,
            -5.056512677995137E101d,
            +2.0902045062932175E102d,
            +6.243669516810509E102d,
            -1.7375090618655787E103d,
            -2.5445477450140954E103d,
            +3.619891246849381E103d,
            +8.90737333900943E103d,
            -2.7897360297480367E104d,
            +1.3725786770437066E105d,
            -8.316530604593264E105d,
            -6.054541568735673E105d,
            +7.523374196797555E105d,
            +1.1475955030427985E107d,
            +1.5260756679495707E107d,
            +7.370294848920685E107d,
            +1.3608995799112174E108d,
            +1.0700758858011432E108d,
            -4.989318918773146E108d,
            -1.6629755787634093E108d,
            +7.635999584053557E109d,
            +1.892621828736983E109d,
            -6.793094743406533E110d,
            -8.160628910742724E110d,
            -7.724219106106896E111d,
            -1.6059226011778748E112d,
            -1.5277127454062126E112d,
            +3.911086668967361E112d,
            +3.529920406834134E113d,
            -4.3991443996021166E113d,
            -1.2631909085915044E114d,
            +3.8656278695544835E114d,
            +1.71845288713123E115d,
            +3.7660598745907915E115d,
            -4.048086182363988E115d,
            +2.3093822298965837E116d,
            -9.684925795536813E116d,
            -3.137992585221854E117d,
            -5.637415935329794E117d,
            -1.5536658521931418E118d,
            -6.336314643222911E118d,
            +8.550658957115427E118d,
            -5.591880480212007E119d,
            +2.4137404318673354E119d,
            -2.631656656397244E120d,
            -7.653117429165879E119d,
            -4.073965591445897E121d,
            +3.634781057940233E121d,
            +4.537273754534966E121d,
            -2.5138919966097735E122d,
            -1.0292817180691822E123d,
            -1.4265564976097062E122d,
            +6.000235114895513E123d,
            +4.186590347846346E124d,
            -1.8950538406321535E124d,
            +7.716762345695022E124d,
            -4.443798187035849E125d,
            -2.268994961992292E125d,
            -2.8169291774231604E126d,
            -2.749127978087685E126d,
            -2.2929764629585683E126d,
            -7.369842361872221E127d,
            +2.81312841469177E128d,
            +2.7856896414497757E128d,
            -3.096733638475319E128d,
            -5.4199510725063615E129d,
            -7.315860999413894E129d,
            +3.6424644535156437E130d,
            -7.886250961456327E130d,
            +5.289988151341401E130d,
            +2.7758613753516344E131d,
            -2.738246981762776E132d,
            -2.2667181460478093E132d,
            -3.614672661225457E131d,
            +2.325337720526947E133d,
            +4.16603235883392E133d,
            -6.50348962894948E133d,
            +3.851445905038431E134d,
            -5.46060534001412E134d,
            +5.4679180659102885E135d,
            -3.037477806841494E135d,
            -3.0417051809209134E136d,
            -6.995964550587914E136d,
            -3.6897084415718804E137d,
            -6.938000231893302E137d,
            +2.403806217004454E138d,
            -3.4552363953199905E138d,
            +7.3409917428393E138d,
            -1.7445917446236717E139d,
            -6.680679913078676E139d,
            -8.193572619487537E139d,
            +5.337290292186291E139d,
            -3.951314467739045E140d,
            -4.4662073456574476E141d,
            +6.249381778908997E141d,
            -2.928362616578011E142d,
            -1.6661676835672304E143d,
            -1.974465323891493E143d,
            +1.3083870531380308E144d,
            -2.382825271750576E144d,
            -5.4826958838142734E144d,
            +1.5340733916570804E145d,
            -3.1327120557842516E145d,
            +1.5790297768522832E146d,
            +1.1518771984292262E146d,
            -4.789917000227385E145d,
            -8.689594184775204E146d,
            +3.0680417869552433E146d,
            +4.877860620031438E147d,
            -3.4650891244084597E148d,
            +1.8702183451052442E149d,
            -3.5727227900139915E148d,
            -1.3457821696677932E150d,
            +3.3212950284273017E149d,
            +7.316033240396569E150d,
            -7.187723217018267E150d,
            -8.537194547485455E150d,
            -1.4561530066010593E152d,
            -7.548155147049997E151d,
            +1.0047353208353007E153d,
            -1.2489460589853119E153d,
            +4.426120229279107E153d,
            -2.5466223330961086E154d,
            +8.831699889789037E154d,
            -2.0258084311749475E155d,
            -5.525009099476396E155d,
            -1.0235056525096769E156d,
            -4.117971654572494E154d,
            -4.7559175309753334E156d,
            -1.4656240137098836E157d,
            -7.675790582869644E157d,
            -1.0126616322947826E158d,
            +7.084865265284368E158d,
            -9.374695893307895E158d,
            +2.05597910889115E159d,
            -7.368602086210704E159d,
            -1.6167825196198978E160d,
            +2.3832096207000712E160d,
            +1.3166970112139726E161d,
            -6.432337568761393E161d,
            +2.9279594746502846E161d,
            +4.8926595743317624E162d,
            +1.2704793774453618E163d,
            -1.1345910784680524E163d,
            +7.75933511025868E163d,
            -1.1441115218462356E163d,
            +5.162248481759758E164d,
            +6.362563919556132E164d,
            -2.8362173224732088E165d,
            -4.342161053332263E165d,
            +4.388125271425036E166d,
            -7.049068240916723E166d,
            +3.8520227881415595E166d,
            +2.9274120974020826E167d,
            -7.500936767542933E167d,
            -6.540181860667302E168d,
            +4.664436780622191E168d,
            -1.436111169285268E169d,
            -1.0407581736224179E170d,
            -2.7670181051374297E170d,
            -6.788169932297778E170d,
            +1.6997387217850427E171d,
            -1.0965324942770584E171d,
            +9.841563119484623E171d,
            +3.175748919314254E172d,
            +2.9621181706527444E172d,
            -3.30101656090905E173d,
            -3.791840683760427E173d,
            -2.841347842666459E174d,
            -7.836327226971707E174d,
            +9.650358667643114E174d,
            +5.9994277301267294E175d,
            -6.0490084078440215E175d,
            -2.8964095485948707E176d,
            +9.916187343252014E175d,
            +2.7535627955313556E176d,
            +3.886891475472745E177d,
            +3.1962472803616787E178d,
            -5.50599549115449E178d,
            +5.672812341879918E178d,
            -3.295268490032475E179d,
            +9.761163062156018E179d,
            +3.107837179570674E180d,
            +3.3894811576571423E179d,
            -5.235397688850367E180d,
            -5.004237248003625E181d,
            -1.7544995191195304E182d,
            +2.645622651144787E182d,
            -3.459885432869825E182d,
            -4.0361435606199565E183d,
            -1.8382923511801317E183d,
            -1.7332235571505177E184d,
            +2.847653455671381E184d,
            +1.7991060813894693E185d,
            -2.0937429891059164E185d,
            +5.744446753652847E185d,
            -2.1349396267483754E184d,
            -1.2542332720182776E186d,
            +3.3730714236579374E186d,
            -5.923734606208998E187d,
            +2.24669039465627E188d,
            -1.2588742703536392E188d,
            +1.474522484905093E189d,
            -2.4006971787803736E189d,
            -3.52597540499141E189d,
            +2.6676722922838097E190d,
            +5.27579825970359E190d,
            +2.1360492104281465E191d,
            +1.9442210982008953E191d,
            -1.4691239161932232E190d,
            +3.8218180377739526E192d,
            +1.9722862688653467E192d,
            +3.047601928063002E193d,
            +1.6747356805175311E193d,
            +7.710512446969693E192d,
            +1.7780021277684035E194d,
            -1.4015110811648513E195d,
            +4.0447634595724164E195d,
            +9.023639664212642E195d,
            +1.976868146639626E196d,
            -9.084495133765657E196d,
            -1.2023077889892748E196d,
            +5.7455368446308694E197d,
            -1.7766273910482863E198d,
            +3.5590470673352285E198d,
            +1.1304970373249033E199d,
            +1.6496143246803731E199d,
            -2.394588390685223E199d,
            -1.4677321100833294E199d,
            -1.1843870433971731E201d,
            -1.8853982316037226E201d,
            +2.8829871423339434E201d,
            +5.369687677705385E200d,
            +1.8356062677502141E202d,
            -1.5544655377217875E203d,
            +2.955364187248884E203d,
            -2.7651059253677425E203d,
            +9.903174064539538E203d,
            -3.284204788892967E204d,
            -1.5843229740595697E205d,
            +5.333371443528904E204d,
            +1.2781631468016048E205d,
            +3.2188292385399854E205d,
            -6.619064395428225E206d,
            +1.291561142865928E207d,
            +1.3142988156905172E207d,
            -1.3841980097978606E208d,
            +6.380177790989479E207d,
            +1.0386032577072378E209d,
            +2.7681631086098026E209d,
            -9.053874899534375E209d,
            +1.2424707839848734E210d,
            +1.045546633850141E211d,
            -1.2448938139338362E211d,
            +7.221902646057552E211d,
            +6.651345415954053E211d,
            -5.8180712702152444E212d,
            +5.275183961165903E212d,
            +5.092753117288608E212d,
            -2.437796532151255E213d,
            +1.3480763914637323E214d,
            +5.619995933180841E214d,
            +2.547000388735681E214d,
            +4.817319356453926E214d,
            -7.897146442236022E215d,
            -7.93844120619577E215d,
            -4.9489938500591624E216d,
            -2.862720607805682E216d,
            -2.9275804461593914E217d,
            -3.411186219855533E217d,
            -2.0110092718356274E218d,
            -8.472642266772353E218d,
            -4.357990742470246E217d,
            +4.793444363780116E219d,
            +1.6544084224626834E220d,
            -6.017988576347111E220d,
            -3.580397221598409E220d,
            -4.7208848667217906E221d,
            -7.724899660259369E221d,
            -2.4459728627968634E222d,
            +3.667348665023154E221d,
            +4.544122762558404E223d,
            -4.0573420909530794E223d,
            -3.2552002992257195E223d,
            -6.488296536838142E224d,
            +1.7544839352461719E224d,
            -4.0873400635183405E225d,
            -8.833499967268279E225d,
            -1.0953484767704112E226d,
            -8.56825295972308E226d,
            -1.8097633115378247E227d,
            -6.171564449018882E227d,
            -4.351843341274115E227d,
            +2.8032429752543687E228d,
            -1.0065901934522996E229d,
            +9.863720960170636E228d,
            -9.481088691357648E229d,
            -1.6843492713373762E229d,
            -1.3282890219894906E230d,
            +6.883577595238845E230d,
            -1.153577281189635E231d,
            -8.009548754642203E231d,
            -4.722612904888278E232d,
            -4.768909872963015E232d,
            +3.2542391242036633E233d,
            +6.513425781583774E233d,
            -1.8889614379831606E233d,
            -2.227647301474917E234d,
            -4.7971208532986115E234d,
            +6.693500938105557E234d,
            -6.587776621471115E234d,
            +3.0099905634916516E236d,
            -4.6694407626686244E235d,
            +2.965546585110978E236d,
            +5.771457643937893E237d,
            -9.029878114318277E237d,
            +8.169926810324408E237d,
            -1.779945804977441E239d,
            +4.1218749988429474E239d,
            +7.201319954099161E239d,
            -1.530845432304069E240d,
            -3.861762510530086E240d,
            -2.4090696463777446E241d,
            -1.8196842273916379E241d,
            -1.7959243076374794E242d,
            -3.7257346819782323E242d,
            +3.413310324247329E242d,
            -2.0406580894051073E243d,
            -1.5335923091350053E243d,
            -1.056727406551016E244d,
            -4.6753408714233723E244d,
            -2.0697130057384643E245d,
            -1.0356006160554071E245d,
            +1.1339195187304043E246d,
            +1.792783182582235E246d,
            +9.599214853681978E245d,
            +1.5367645598839362E247d,
            +2.934570385464815E247d,
            -1.6411525886171892E248d,
            +2.2638862982382794E248d,
            -1.2268014119628852E249d,
            +4.737693450915584E247d,
            +6.3818993714899675E249d,
            +1.2639113706171572E250d,
            -4.011320021817099E249d,
            -5.2744376732859406E250d,
            -3.732266217624991E251d,
            +1.7591819833844019E252d,
            -3.292458622014749E252d,
            -9.161340309319204E252d,
            -1.728610646009749E253d,
            +1.1698424008604891E254d,
            -1.8494343291160577E254d,
            +2.0568656302182574E254d,
            +1.0537591246531136E255d,
            +1.803052068234866E254d,
            -1.053036399720808E256d,
            +2.1836166619192508E256d,
            +1.0368403169781264E257d,
            -2.0648015610276362E257d,
            +8.426174035728768E257d,
            -1.3577357192972777E258d,
            +2.1313950901331177E258d,
            +8.919141843592823E258d,
            -1.1800039972549816E259d,
            -1.1878772398311421E260d,
            -1.538273497873993E260d,
            -4.51305093266001E260d,
            +1.1241179396053055E261d,
            +6.154786508667658E261d,
            -1.0626125049032536E262d,
            -1.8908603201210102E262d,
            -4.571195152299358E262d,
            +1.526100002923062E263d,
            -9.457084582570225E263d,
            -1.5460500618825853E264d,
            -5.598276199126451E264d,
            -1.2074097381167957E265d,
            -3.015972957475025E265d,
            +1.4345106852061226E265d,
            +8.28479585346867E265d,
            -3.118741081244705E266d,
            -1.2054747399765794E266d,
            +3.4454766202661184E267d,
            +1.1279135096919439E268d,
            +1.2066382528772518E268d,
            +1.1984128162292276E269d,
            +3.685169705587367E268d,
            +6.570047690198998E269d,
            +1.8836492887460383E270d,
            +7.4364594917181125E270d,
            +1.2773080633674971E271d,
            +1.8928981707279692E271d,
            +4.039437286589528E271d,
            +1.785277385538302E272d,
            -6.017681359527226E272d,
            +1.9716943051755635E273d,
            -8.772048092842086E271d,
            +1.5645672698520312E274d,
            -3.7979660725865874E274d,
            +5.324902289537048E274d,
            -1.8806716685063293E274d,
            +9.320900373401115E275d,
            +1.4615985810260016E275d,
            +8.321226457219046E276d,
            -4.608112855795952E276d,
            -3.476352191116455E277d,
            +5.266381689434054E277d,
            -9.622106063561645E277d,
            +4.1719443712336026E278d,
            +4.222411269063919E279d,
            -6.714376022102489E279d,
            -1.0732735585199074E280d,
            -2.5866883048437488E280d,
            -1.1306860837934988E281d,
            +3.690690354793168E281d,
            -5.5299180508885456E281d,
            +2.7006726968568243E282d,
            +4.135457669031131E282d,
            +2.8401159516008676E283d,
            +5.127265762024798E283d,
            -3.4893601256685762E283d,
            -1.145160459652136E283d,
            +2.1742808735341656E284d,
            +4.656972469326391E285d,
            +7.672307991205681E285d,
            +1.5781599575584034E286d,
            +4.218682431618625E286d,
            -2.4602260687026867E287d,
            +2.7211316452521414E287d,
            -1.8740018211089393E288d,
            +2.6367639658206183E288d,
            -3.102678910525039E288d,
            +1.1992295328636466E289d,
            +6.8190133180135345E289d,
            +5.783203879030497E289d,
            +5.171047077293295E290d,
            +1.8396930096213817E290d,
            +1.4977047507315718E290d,
            +1.0672499803427623E292d,
            +3.3310942289102464E291d,
            -7.962256961838823E292d,
            +1.7396889119023863E293d,
            +3.8072183820435085E293d,
            +2.2772059538865722E294d,
            -2.0549866377878678E294d,
            -1.2277120342804144E295d,
            -3.609949022969024E295d,
            +1.1479863663699871E296d,
            -1.5314373779304356E296d,
            -2.2537635160762597E296d,
            -6.1370690793508674E296d,
            -4.996854125490041E297d,
            -6.883499809714189E297d,
            -2.595456638706416E298d,
            -1.1892631528580186E299d,
            -1.4672600326020399E299d,
            -3.200068509818696E299d,
            -7.126913872617518E298d,
            -3.3655587417265094E300d,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
            Double.NaN,
    };

    private static final double[] EXP_FRAC_A = new double[]{
            +1.0d,
            +1.0009770393371582d,
            +1.0019550323486328d,
            +1.0029339790344238d,
            +1.0039138793945312d,
            +1.004894733428955d,
            +1.0058765411376953d,
            +1.006859302520752d,
            +1.007843017578125d,
            +1.0088276863098145d,
            +1.0098135471343994d,
            +1.0108001232147217d,
            +1.0117876529693604d,
            +1.0127761363983154d,
            +1.013765811920166d,
            +1.014756202697754d,
            +1.0157477855682373d,
            +1.016740083694458d,
            +1.0177335739135742d,
            +1.0187277793884277d,
            +1.0197231769561768d,
            +1.0207195281982422d,
            +1.021716833114624d,
            +1.0227150917053223d,
            +1.023714303970337d,
            +1.024714469909668d,
            +1.0257158279418945d,
            +1.0267179012298584d,
            +1.0277209281921387d,
            +1.0287251472473145d,
            +1.0297303199768066d,
            +1.0307364463806152d,
            +1.0317435264587402d,
            +1.0327515602111816d,
            +1.0337605476379395d,
            +1.0347704887390137d,
            +1.0357816219329834d,
            +1.0367934703826904d,
            +1.037806510925293d,
            +1.038820505142212d,
            +1.0398354530334473d,
            +1.040851354598999d,
            +1.0418684482574463d,
            +1.0428862571716309d,
            +1.043905258178711d,
            +1.0449252128601074d,
            +1.0459461212158203d,
            +1.0469679832458496d,
            +1.0479910373687744d,
            +1.0490150451660156d,
            +1.0500397682189941d,
            +1.0510656833648682d,
            +1.0520927906036377d,
            +1.0531206130981445d,
            +1.0541496276855469d,
            +1.0551795959472656d,
            +1.0562105178833008d,
            +1.0572423934936523d,
            +1.0582754611968994d,
            +1.059309482574463d,
            +1.0603444576263428d,
            +1.061380386352539d,
            +1.0624175071716309d,
            +1.06345534324646d,
            +1.0644943714141846d,
            +1.0655345916748047d,
            +1.066575527191162d,
            +1.067617654800415d,
            +1.0686607360839844d,
            +1.0697050094604492d,
            +1.0707499980926514d,
            +1.071796178817749d,
            +1.072843313217163d,
            +1.0738916397094727d,
            +1.0749409198760986d,
            +1.075991153717041d,
            +1.0770423412322998d,
            +1.078094720840454d,
            +1.0791480541229248d,
            +1.080202341079712d,
            +1.0812578201293945d,
            +1.0823142528533936d,
            +1.083371639251709d,
            +1.08443021774292d,
            +1.0854897499084473d,
            +1.086550235748291d,
            +1.0876119136810303d,
            +1.088674545288086d,
            +1.089738130569458d,
            +1.0908029079437256d,
            +1.0918686389923096d,
            +1.092935562133789d,
            +1.094003438949585d,
            +1.0950722694396973d,
            +1.096142053604126d,
            +1.0972130298614502d,
            +1.09828519821167d,
            +1.099358320236206d,
            +1.1004323959350586d,
            +1.1015074253082275d,
            +1.102583646774292d,
            +1.103661060333252d,
            +1.1047391891479492d,
            +1.105818748474121d,
            +1.1068990230560303d,
            +1.107980489730835d,
            +1.1090631484985352d,
            +1.1101467609405518d,
            +1.1112313270568848d,
            +1.1123170852661133d,
            +1.1134037971496582d,
            +1.1144917011260986d,
            +1.1155805587768555d,
            +1.1166706085205078d,
            +1.1177616119384766d,
            +1.1188538074493408d,
            +1.1199469566345215d,
            +1.1210410594940186d,
            +1.1221363544464111d,
            +1.1232328414916992d,
            +1.1243302822113037d,
            +1.1254286766052246d,
            +1.126528263092041d,
            +1.127629041671753d,
            +1.1287307739257812d,
            +1.129833459854126d,
            +1.1309373378753662d,
            +1.132042407989502d,
            +1.133148431777954d,
            +1.1342556476593018d,
            +1.1353638172149658d,
            +1.1364731788635254d,
            +1.1375834941864014d,
            +1.1386950016021729d,
            +1.1398074626922607d,
            +1.1409211158752441d,
            +1.142035961151123d,
            +1.1431517601013184d,
            +1.14426851272583d,
            +1.1453864574432373d,
            +1.14650559425354d,
            +1.1476259231567383d,
            +1.148747205734253d,
            +1.149869441986084d,
            +1.1509928703308105d,
            +1.1521174907684326d,
            +1.153243064880371d,
            +1.154369831085205d,
            +1.1554977893829346d,
            +1.1566267013549805d,
            +1.1577568054199219d,
            +1.1588881015777588d,
            +1.160020351409912d,
            +1.161153793334961d,
            +1.1622881889343262d,
            +1.163423776626587d,
            +1.1645605564117432d,
            +1.1656982898712158d,
            +1.166837215423584d,
            +1.1679773330688477d,
            +1.1691184043884277d,
            +1.1702606678009033d,
            +1.1714041233062744d,
            +1.172548532485962d,
            +1.173694133758545d,
            +1.1748409271240234d,
            +1.1759889125823975d,
            +1.177137851715088d,
            +1.1782879829406738d,
            +1.1794393062591553d,
            +1.1805915832519531d,
            +1.1817450523376465d,
            +1.1828997135162354d,
            +1.1840553283691406d,
            +1.1852121353149414d,
            +1.1863701343536377d,
            +1.1875293254852295d,
            +1.1886897087097168d,
            +1.1898510456085205d,
            +1.1910135746002197d,
            +1.1921772956848145d,
            +1.1933419704437256d,
            +1.1945080757141113d,
            +1.1956751346588135d,
            +1.1968433856964111d,
            +1.1980125904083252d,
            +1.1991832256317139d,
            +1.200354814529419d,
            +1.2015275955200195d,
            +1.2027015686035156d,
            +1.2038767337799072d,
            +1.2050528526306152d,
            +1.2062301635742188d,
            +1.2074086666107178d,
            +1.2085883617401123d,
            +1.2097692489624023d,
            +1.210951328277588d,
            +1.2121343612670898d,
            +1.2133188247680664d,
            +1.2145042419433594d,
            +1.2156908512115479d,
            +1.2168786525726318d,
            +1.2180676460266113d,
            +1.2192575931549072d,
            +1.2204489707946777d,
            +1.2216413021087646d,
            +1.222834825515747d,
            +1.224029779434204d,
            +1.2252256870269775d,
            +1.2264227867126465d,
            +1.227621078491211d,
            +1.2288203239440918d,
            +1.2300209999084473d,
            +1.2312228679656982d,
            +1.2324256896972656d,
            +1.2336299419403076d,
            +1.234835147857666d,
            +1.23604154586792d,
            +1.2372493743896484d,
            +1.2384581565856934d,
            +1.2396681308746338d,
            +1.2408792972564697d,
            +1.2420918941497803d,
            +1.2433054447174072d,
            +1.2445201873779297d,
            +1.2457361221313477d,
            +1.2469532489776611d,
            +1.2481715679168701d,
            +1.2493910789489746d,
            +1.2506117820739746d,
            +1.2518336772918701d,
            +1.2530567646026611d,
            +1.2542810440063477d,
            +1.2555065155029297d,
            +1.2567331790924072d,
            +1.2579610347747803d,
            +1.2591900825500488d,
            +1.260420322418213d,
            +1.2616519927978516d,
            +1.2628846168518066d,
            +1.2641184329986572d,
            +1.2653534412384033d,
            +1.266589879989624d,
            +1.2678272724151611d,
            +1.2690660953521729d,
            +1.27030611038208d,
            +1.2715470790863037d,
            +1.272789478302002d,
            +1.2740330696105957d,
            +1.275277853012085d,
            +1.2765238285064697d,
            +1.27777099609375d,
            +1.2790195941925049d,
            +1.2802691459655762d,
            +1.281519889831543d,
            +1.2827720642089844d,
            +1.2840254306793213d,
            +1.2852799892425537d,
            +1.2865357398986816d,
            +1.287792682647705d,
            +1.2890510559082031d,
            +1.2903103828430176d,
            +1.2915711402893066d,
            +1.2928330898284912d,
            +1.2940962314605713d,
            +1.2953605651855469d,
            +1.296626091003418d,
            +1.2978930473327637d,
            +1.2991611957550049d,
            +1.3004305362701416d,
            +1.3017010688781738d,
            +1.3029727935791016d,
            +1.304245948791504d,
            +1.3055200576782227d,
            +1.3067958354949951d,
            +1.308072566986084d,
            +1.3093504905700684d,
            +1.3106298446655273d,
            +1.3119103908538818d,
            +1.3131921291351318d,
            +1.3144752979278564d,
            +1.3157594203948975d,
            +1.317044973373413d,
            +1.3183319568634033d,
            +1.31961989402771d,
            +1.3209092617034912d,
            +1.322199821472168d,
            +1.3234915733337402d,
            +1.324784755706787d,
            +1.3260791301727295d,
            +1.3273746967315674d,
            +1.3286716938018799d,
            +1.329969882965088d,
            +1.3312692642211914d,
            +1.3325698375701904d,
            +1.333871841430664d,
            +1.3351752758026123d,
            +1.336479663848877d,
            +1.3377854824066162d,
            +1.339092493057251d,
            +1.3404009342193604d,
            +1.3417105674743652d,
            +1.3430213928222656d,
            +1.3443336486816406d,
            +1.3456470966339111d,
            +1.3469617366790771d,
            +1.3482778072357178d,
            +1.349595069885254d,
            +1.3509137630462646d,
            +1.352233648300171d,
            +1.3535549640655518d,
            +1.3548774719238281d,
            +1.356201171875d,
            +1.3575263023376465d,
            +1.3588526248931885d,
            +1.360180139541626d,
            +1.361509084701538d,
            +1.3628394603729248d,
            +1.364171028137207d,
            +1.3655037879943848d,
            +1.366837978363037d,
            +1.368173360824585d,
            +1.3695101737976074d,
            +1.3708481788635254d,
            +1.372187614440918d,
            +1.373528242111206d,
            +1.3748703002929688d,
            +1.376213550567627d,
            +1.3775582313537598d,
            +1.378904104232788d,
            +1.380251407623291d,
            +1.3815999031066895d,
            +1.3829498291015625d,
            +1.384300947189331d,
            +1.3856534957885742d,
            +1.387007236480713d,
            +1.3883624076843262d,
            +1.389719009399414d,
            +1.3910768032073975d,
            +1.3924360275268555d,
            +1.393796443939209d,
            +1.395158290863037d,
            +1.3965213298797607d,
            +1.397885799407959d,
            +1.3992514610290527d,
            +1.4006187915802002d,
            +1.401987075805664d,
            +1.4033570289611816d,
            +1.4047281742095947d,
            +1.4061005115509033d,
            +1.4074742794036865d,
            +1.4088494777679443d,
            +1.4102261066436768d,
            +1.4116039276123047d,
            +1.4129831790924072d,
            +1.4143636226654053d,
            +1.415745496749878d,
            +1.4171288013458252d,
            +1.418513298034668d,
            +1.4198992252349854d,
            +1.4212865829467773d,
            +1.4226751327514648d,
            +1.424065351486206d,
            +1.4254565238952637d,
            +1.426849365234375d,
            +1.4282433986663818d,
            +1.4296388626098633d,
            +1.4310357570648193d,
            +1.432433843612671d,
            +1.433833360671997d,
            +1.4352343082427979d,
            +1.4366366863250732d,
            +1.4380402565002441d,
            +1.4394452571868896d,
            +1.4408516883850098d,
            +1.4422595500946045d,
            +1.4436686038970947d,
            +1.4450790882110596d,
            +1.446491003036499d,
            +1.447904348373413d,
            +1.4493188858032227d,
            +1.450735092163086d,
            +1.4521524906158447d,
            +1.4535713195800781d,
            +1.454991340637207d,
            +1.4564130306243896d,
            +1.4578359127044678d,
            +1.4592602252960205d,
            +1.460686206817627d,
            +1.4621131420135498d,
            +1.4635417461395264d,
            +1.4649717807769775d,
            +1.4664030075073242d,
            +1.4678359031677246d,
            +1.4692699909210205d,
            +1.470705509185791d,
            +1.4721424579620361d,
            +1.4735808372497559d,
            +1.475020408630371d,
            +1.47646164894104d,
            +1.4779040813446045d,
            +1.4793481826782227d,
            +1.4807934761047363d,
            +1.4822404384613037d,
            +1.4836885929107666d,
            +1.485138177871704d,
            +1.4865891933441162d,
            +1.488041639328003d,
            +1.4894955158233643d,
            +1.4909508228302002d,
            +1.4924075603485107d,
            +1.493865728378296d,
            +1.4953253269195557d,
            +1.49678635597229d,
            +1.49824857711792d,
            +1.4997124671936035d,
            +1.5011777877807617d,
            +1.5026445388793945d,
            +1.504112720489502d,
            +1.505582332611084d,
            +1.5070531368255615d,
            +1.5085256099700928d,
            +1.5099995136260986d,
            +1.511474847793579d,
            +1.5129516124725342d,
            +1.5144298076629639d,
            +1.5159096717834473d,
            +1.5173907279968262d,
            +1.5188732147216797d,
            +1.5203571319580078d,
            +1.5218427181243896d,
            +1.523329496383667d,
            +1.524817943572998d,
            +1.5263078212738037d,
            +1.5277988910675049d,
            +1.5292916297912598d,
            +1.5307857990264893d,
            +1.5322813987731934d,
            +1.5337786674499512d,
            +1.5352771282196045d,
            +1.5367772579193115d,
            +1.538278579711914d,
            +1.5397815704345703d,
            +1.5412859916687012d,
            +1.5427920818328857d,
            +1.5442993640899658d,
            +1.5458080768585205d,
            +1.547318458557129d,
            +1.548830270767212d,
            +1.5503435134887695d,
            +1.5518584251403809d,
            +1.5533745288848877d,
            +1.5548923015594482d,
            +1.5564115047454834d,
            +1.5579321384429932d,
            +1.5594542026519775d,
            +1.5609779357910156d,
            +1.5625030994415283d,
            +1.5640296936035156d,
            +1.5655577182769775d,
            +1.5670874118804932d,
            +1.5686185359954834d,
            +1.5701510906219482d,
            +1.5716853141784668d,
            +1.5732207298278809d,
            +1.5747578144073486d,
            +1.5762965679168701d,
            +1.577836513519287d,
            +1.5793781280517578d,
            +1.5809214115142822d,
            +1.5824658870697021d,
            +1.5840120315551758d,
            +1.5855598449707031d,
            +1.587108850479126d,
            +1.5886595249176025d,
            +1.5902118682861328d,
            +1.5917654037475586d,
            +1.593320608139038d,
            +1.5948774814605713d,
            +1.596435785293579d,
            +1.5979955196380615d,
            +1.5995566844940186d,
            +1.6011195182800293d,
            +1.6026840209960938d,
            +1.6042497158050537d,
            +1.6058173179626465d,
            +1.6073861122131348d,
            +1.6089565753936768d,
            +1.6105287075042725d,
            +1.6121022701263428d,
            +1.6136772632598877d,
            +1.6152539253234863d,
            +1.6168320178985596d,
            +1.6184117794036865d,
            +1.619992971420288d,
            +1.6215758323669434d,
            +1.6231601238250732d,
            +1.6247460842132568d,
            +1.626333475112915d,
            +1.627922534942627d,
            +1.6295130252838135d,
            +1.6311051845550537d,
            +1.6326987743377686d,
            +1.634294033050537d,
            +1.6358907222747803d,
            +1.6374890804290771d,
            +1.6390891075134277d,
            +1.640690565109253d,
            +1.6422934532165527d,
            +1.6438980102539062d,
            +1.6455042362213135d,
            +1.6471118927001953d,
            +1.6487212181091309d,
            +1.6503322124481201d,
            +1.651944637298584d,
            +1.6535584926605225d,
            +1.6551742553710938d,
            +1.6567914485931396d,
            +1.6584100723266602d,
            +1.6600303649902344d,
            +1.6616523265838623d,
            +1.663275957107544d,
            +1.6649010181427002d,
            +1.666527509689331d,
            +1.6681559085845947d,
            +1.669785737991333d,
            +1.671417236328125d,
            +1.6730501651763916d,
            +1.674684762954712d,
            +1.676321029663086d,
            +1.6779589653015137d,
            +1.679598331451416d,
            +1.681239366531372d,
            +1.6828820705413818d,
            +1.6845262050628662d,
            +1.6861720085144043d,
            +1.687819480895996d,
            +1.6894686222076416d,
            +1.6911191940307617d,
            +1.6927716732025146d,
            +1.6944255828857422d,
            +1.6960809230804443d,
            +1.6977381706237793d,
            +1.6993968486785889d,
            +1.7010571956634521d,
            +1.7027192115783691d,
            +1.7043828964233398d,
            +1.7060482501983643d,
            +1.7077150344848633d,
            +1.709383487701416d,
            +1.7110536098480225d,
            +1.7127254009246826d,
            +1.7143988609313965d,
            +1.716073989868164d,
            +1.7177505493164062d,
            +1.7194287776947021d,
            +1.7211089134216309d,
            +1.7227904796600342d,
            +1.7244737148284912d,
            +1.726158618927002d,
            +1.7278449535369873d,
            +1.7295331954956055d,
            +1.7312231063842773d,
            +1.7329144477844238d,
            +1.7346076965332031d,
            +1.736302375793457d,
            +1.7379989624023438d,
            +1.739696979522705d,
            +1.7413966655731201d,
            +1.7430980205535889d,
            +1.7448012828826904d,
            +1.7465059757232666d,
            +1.7482123374938965d,
            +1.74992036819458d,
            +1.7516300678253174d,
            +1.7533416748046875d,
            +1.7550547122955322d,
            +1.7567694187164307d,
            +1.7584857940673828d,
            +1.7602040767669678d,
            +1.7619237899780273d,
            +1.7636451721191406d,
            +1.7653684616088867d,
            +1.7670931816101074d,
            +1.768819808959961d,
            +1.770547866821289d,
            +1.77227783203125d,
            +1.7740094661712646d,
            +1.775742769241333d,
            +1.777477741241455d,
            +1.7792143821716309d,
            +1.7809526920318604d,
            +1.7826926708221436d,
            +1.7844345569610596d,
            +1.7861778736114502d,
            +1.7879230976104736d,
            +1.7896699905395508d,
            +1.7914185523986816d,
            +1.7931687831878662d,
            +1.7949209213256836d,
            +1.7966744899749756d,
            +1.7984299659729004d,
            +1.800187110900879d,
            +1.8019459247589111d,
            +1.8037066459655762d,
            +1.8054687976837158d,
            +1.8072328567504883d,
            +1.8089985847473145d,
            +1.8107659816741943d,
            +1.812535285949707d,
            +1.8143062591552734d,
            +1.8160789012908936d,
            +1.8178532123565674d,
            +1.819629430770874d,
            +1.8214070796966553d,
            +1.8231868743896484d,
            +1.8249680995941162d,
            +1.8267512321472168d,
            +1.828536033630371d,
            +1.830322504043579d,
            +1.83211088180542d,
            +1.8339009284973145d,
            +1.8356926441192627d,
            +1.8374862670898438d,
            +1.8392815589904785d,
            +1.841078519821167d,
            +1.8428773880004883d,
            +1.8446779251098633d,
            +1.846480131149292d,
            +1.8482842445373535d,
            +1.8500902652740479d,
            +1.8518977165222168d,
            +1.8537070751190186d,
            +1.8555183410644531d,
            +1.8573312759399414d,
            +1.8591458797454834d,
            +1.8609623908996582d,
            +1.8627805709838867d,
            +1.864600658416748d,
            +1.866422414779663d,
            +1.8682458400726318d,
            +1.8700714111328125d,
            +1.8718984127044678d,
            +1.8737273216247559d,
            +1.8755581378936768d,
            +1.8773906230926514d,
            +1.8792247772216797d,
            +1.8810608386993408d,
            +1.8828988075256348d,
            +1.8847384452819824d,
            +1.886579990386963d,
            +1.888423204421997d,
            +1.890268325805664d,
            +1.8921151161193848d,
            +1.8939638137817383d,
            +1.8958141803741455d,
            +1.8976664543151855d,
            +1.8995206356048584d,
            +1.901376485824585d,
            +1.9032342433929443d,
            +1.9050939083099365d,
            +1.9069552421569824d,
            +1.908818244934082d,
            +1.9106833934783936d,
            +1.9125502109527588d,
            +1.9144186973571777d,
            +1.9162893295288086d,
            +1.9181616306304932d,
            +1.9200356006622314d,
            +1.9219114780426025d,
            +1.9237892627716064d,
            +1.9256689548492432d,
            +1.9275505542755127d,
            +1.929433822631836d,
            +1.931318759918213d,
            +1.9332058429718018d,
            +1.9350945949554443d,
            +1.9369852542877197d,
            +1.938877820968628d,
            +1.940772294998169d,
            +1.9426684379577637d,
            +1.9445664882659912d,
            +1.9464664459228516d,
            +1.9483680725097656d,
            +1.9502718448638916d,
            +1.9521772861480713d,
            +1.9540846347808838d,
            +1.955993890762329d,
            +1.9579050540924072d,
            +1.959817886352539d,
            +1.9617326259613037d,
            +1.9636495113372803d,
            +1.9655680656433105d,
            +1.9674885272979736d,
            +1.9694106578826904d,
            +1.9713349342346191d,
            +1.9732608795166016d,
            +1.975188970565796d,
            +1.977118730545044d,
            +1.9790503978729248d,
            +1.9809842109680176d,
            +1.982919692993164d,
            +1.9848570823669434d,
            +1.9867963790893555d,
            +1.9887375831604004d,
            +1.990680456161499d,
            +1.9926254749298096d,
            +1.994572401046753d,
            +1.996521234512329d,
            +1.998471736907959d,
            +2.000424385070801d,
            +2.0023789405822754d,
            +2.004335403442383d,
            +2.006293773651123d,
            +2.008254051208496d,
            +2.010216236114502d,
            +2.0121798515319824d,
            +2.014145851135254d,
            +2.016113758087158d,
            +2.0180835723876953d,
            +2.0200552940368652d,
            +2.022029399871826d,
            +2.0240049362182617d,
            +2.02598237991333d,
            +2.0279617309570312d,
            +2.0299429893493652d,
            +2.0319266319274902d,
            +2.03391170501709d,
            +2.0358991622924805d,
            +2.0378880500793457d,
            +2.039879322052002d,
            +2.041872501373291d,
            +2.0438671112060547d,
            +2.0458641052246094d,
            +2.047863006591797d,
            +2.049863815307617d,
            +2.0518670082092285d,
            +2.0538716316223145d,
            +2.055878162384033d,
            +2.057887077331543d,
            +2.0598974227905273d,
            +2.0619101524353027d,
            +2.063924789428711d,
            +2.065941333770752d,
            +2.067959785461426d,
            +2.0699801445007324d,
            +2.07200288772583d,
            +2.0740270614624023d,
            +2.0760536193847656d,
            +2.0780820846557617d,
            +2.0801124572753906d,
            +2.0821447372436523d,
            +2.084178924560547d,
            +2.0862154960632324d,
            +2.0882534980773926d,
            +2.0902938842773438d,
            +2.0923361778259277d,
            +2.0943803787231445d,
            +2.0964269638061523d,
            +2.0984749794006348d,
            +2.100525379180908d,
            +2.1025776863098145d,
            +2.1046319007873535d,
            +2.1066884994506836d,
            +2.1087465286254883d,
            +2.110806941986084d,
            +2.1128692626953125d,
            +2.114933490753174d,
            +2.117000102996826d,
            +2.1190686225891113d,
            +2.1211390495300293d,
            +2.12321138381958d,
            +2.1252856254577637d,
            +2.1273622512817383d,
            +2.1294407844543457d,
            +2.131521224975586d,
            +2.133604049682617d,
            +2.135688304901123d,
            +2.13777494430542d,
            +2.139863967895508d,
            +2.1419544219970703d,
            +2.144047260284424d,
            +2.14614200592041d,
            +2.1482391357421875d,
            +2.1503376960754395d,
            +2.1524391174316406d,
            +2.1545419692993164d,
            +2.156647205352783d,
            +2.1587538719177246d,
            +2.1608633995056152d,
            +2.1629743576049805d,
            +2.1650876998901367d,
            +2.167203426361084d,
            +2.169320583343506d,
            +2.1714401245117188d,
            +2.1735615730285645d,
            +2.175685405731201d,
            +2.1778111457824707d,
            +2.179938793182373d,
            +2.1820688247680664d,
            +2.1842007637023926d,
            +2.1863350868225098d,
            +2.1884708404541016d,
            +2.1906094551086426d,
            +2.192749500274658d,
            +2.194891929626465d,
            +2.1970362663269043d,
            +2.1991829872131348d,
            +2.201331615447998d,
            +2.2034826278686523d,
            +2.2056355476379395d,
            +2.2077903747558594d,
            +2.2099475860595703d,
            +2.212106704711914d,
            +2.214268207550049d,
            +2.2164316177368164d,
            +2.218596935272217d,
            +2.220764636993408d,
            +2.2229342460632324d,
            +2.2251062393188477d,
            +2.2272801399230957d,
            +2.2294564247131348d,
            +2.2316346168518066d,
            +2.2338151931762695d,
            +2.2359976768493652d,
            +2.2381820678710938d,
            +2.2403693199157715d,
            +2.242558002471924d,
            +2.244749069213867d,
            +2.2469425201416016d,
            +2.2491378784179688d,
            +2.2513351440429688d,
            +2.2535347938537598d,
            +2.2557363510131836d,
            +2.2579402923583984d,
            +2.2601466178894043d,
            +2.262354850769043d,
            +2.2645654678344727d,
            +2.266777992248535d,
            +2.2689924240112305d,
            +2.271209716796875d,
            +2.273428440093994d,
            +2.2756495475769043d,
            +2.2778730392456055d,
            +2.2800989151000977d,
            +2.2823266983032227d,
            +2.2845563888549805d,
            +2.2867884635925293d,
            +2.289022922515869d,
            +2.291259288787842d,
            +2.2934980392456055d,
            +2.295738697052002d,
            +2.2979817390441895d,
            +2.300227165222168d,
            +2.3024744987487793d,
            +2.3047242164611816d,
            +2.306975841522217d,
            +2.309229850769043d,
            +2.31148624420166d,
            +2.31374454498291d,
            +2.316005229949951d,
            +2.318267822265625d,
            +2.32053279876709d,
            +2.3228001594543457d,
            +2.3250694274902344d,
            +2.3273415565490723d,
            +2.3296151161193848d,
            +2.3318915367126465d,
            +2.334169864654541d,
            +2.3364500999450684d,
            +2.338733196258545d,
            +2.3410181999206543d,
            +2.3433055877685547d,
            +2.345594882965088d,
            +2.347886562347412d,
            +2.3501806259155273d,
            +2.3524770736694336d,
            +2.3547754287719727d,
            +2.3570761680603027d,
            +2.3593788146972656d,
            +2.3616843223571777d,
            +2.3639917373657227d,
            +2.3663015365600586d,
            +2.3686132431030273d,
            +2.370927333831787d,
            +2.373243808746338d,
            +2.3755626678466797d,
            +2.3778839111328125d,
            +2.380207061767578d,
            +2.3825325965881348d,
            +2.3848605155944824d,
            +2.387190818786621d,
            +2.3895230293273926d,
            +2.391857624053955d,
            +2.3941946029663086d,
            +2.396533966064453d,
            +2.3988752365112305d,
            +2.401218891143799d,
            +2.4035654067993164d,
            +2.4059133529663086d,
            +2.40826416015625d,
            +2.4106173515319824d,
            +2.4129724502563477d,
            +2.415329933166504d,
            +2.417689800262451d,
            +2.4200520515441895d,
            +2.4224166870117188d,
            +2.424783229827881d,
            +2.427152633666992d,
            +2.4295239448547363d,
            +2.4318976402282715d,
            +2.4342737197875977d,
            +2.436652183532715d,
            +2.439032554626465d,
            +2.441415786743164d,
            +2.4438014030456543d,
            +2.4461889266967773d,
            +2.4485788345336914d,
            +2.4509711265563965d,
            +2.4533658027648926d,
            +2.4557628631591797d,
            +2.458162307739258d,
            +2.460564136505127d,
            +2.462968349456787d,
            +2.46537446975708d,
            +2.4677834510803223d,
            +2.4701943397521973d,
            +2.4726080894470215d,
            +2.4750237464904785d,
            +2.4774417877197266d,
            +2.479862689971924d,
            +2.482285499572754d,
            +2.484710693359375d,
            +2.487138271331787d,
            +2.4895682334899902d,
            +2.4920010566711426d,
            +2.4944357872009277d,
            +2.496872901916504d,
            +2.499312400817871d,
            +2.5017542839050293d,
            +2.5041985511779785d,
            +2.5066452026367188d,
            +2.50909423828125d,
            +2.5115456581115723d,
            +2.5139999389648438d,
            +2.516456127166748d,
            +2.5189146995544434d,
            +2.5213756561279297d,
            +2.5238394737243652d,
            +2.5263051986694336d,
            +2.528773307800293d,
            +2.5312442779541016d,
            +2.533717155456543d,
            +2.5361928939819336d,
            +2.538670539855957d,
            +2.5411510467529297d,
            +2.5436339378356934d,
            +2.546119213104248d,
            +2.5486068725585938d,
            +2.5510969161987305d,
            +2.553589344024658d,
            +2.556084632873535d,
            +2.558581829071045d,
            +2.5610814094543457d,
            +2.5635838508605957d,
            +2.5660886764526367d,
            +2.5685958862304688d,
            +2.571105480194092d,
            +2.573617458343506d,
            +2.576131820678711d,
            +2.5786490440368652d,
            +2.5811686515808105d,
            +2.5836901664733887d,
            +2.586214542388916d,
            +2.5887417793273926d,
            +2.591270923614502d,
            +2.5938024520874023d,
            +2.596336841583252d,
            +2.5988736152648926d,
            +2.601412773132324d,
            +2.603954315185547d,
            +2.6064987182617188d,
            +2.6090455055236816d,
            +2.6115946769714355d,
            +2.6141462326049805d,
            +2.6167001724243164d,
            +2.6192569732666016d,
            +2.6218161582946777d,
            +2.624377727508545d,
            +2.626941680908203d,
            +2.6295084953308105d,
            +2.632077217102051d,
            +2.6346492767333984d,
            +2.637223243713379d,
            +2.6398000717163086d,
            +2.6423792839050293d,
            +2.644960880279541d,
            +2.6475448608398438d,
            +2.6501317024230957d,
            +2.6527209281921387d,
            +2.655313014984131d,
            +2.657907009124756d,
            +2.6605043411254883d,
            +2.6631035804748535d,
            +2.665705680847168d,
            +2.6683101654052734d,
            +2.67091703414917d,
            +2.6735267639160156d,
            +2.6761388778686523d,
            +2.67875337600708d,
            +2.681370735168457d,
            +2.683990478515625d,
            +2.686613082885742d,
            +2.689237594604492d,
            +2.6918654441833496d,
            +2.69449520111084d,
            +2.6971278190612793d,
            +2.699763298034668d,
            +2.7024011611938477d,
            +2.7050414085388184d,
            +2.70768404006958d,
            +2.710329532623291d,
            +2.712977886199951d,
            +2.7156286239624023d,
            +2.7182817459106445d,
    };

    /**
     * Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
     */
    private static final double[] EXP_FRAC_B = new double[]{
            +0.0d,
            +1.552583321178453E-10d,
            +1.2423699995465188E-9d,
            +4.194022929828008E-9d,
            +9.94381632344361E-9d,
            +1.9426261544163577E-8d,
            +3.3576783010266685E-8d,
            +5.3331719086630523E-8d,
            +7.962832297769345E-8d,
            +1.1340476362128895E-7d,
            -8.281845251820919E-8d,
            -3.126416414805498E-8d,
            +3.058997113995161E-8d,
            +1.0368579417304741E-7d,
            -4.9452513107409435E-8d,
            +4.8955889659397494E-8d,
            -7.698155155722897E-8d,
            +5.051784853384516E-8d,
            -4.443661736519001E-8d,
            +1.1593958457401774E-7d,
            +5.575759739697068E-8d,
            +1.4385227981629147E-8d,
            -7.227368462584163E-9d,
            -8.129108387083023E-9d,
            +1.263202100290635E-8d,
            +5.600896265625552E-8d,
            -1.154629885168314E-7d,
            -2.399186832888246E-8d,
            +9.295948298604103E-8d,
            -2.070841011504222E-9d,
            -6.97066538508643E-8d,
            -1.0898941254272996E-7d,
            -1.1895963756343625E-7d,
            -9.865691193993138E-8d,
            -4.711988033385175E-8d,
            +3.6613751875298095E-8d,
            -8.491135959370133E-8d,
            +6.610611940107793E-8d,
            +1.3794148633283659E-8d,
            -2.462631860370667E-9d,
            +1.830278273495162E-8d,
            +7.705834203598065E-8d,
            -6.364563771711373E-8d,
            +7.39978436695387E-8d,
            +1.4122417557484554E-8d,
            -3.881598887298574E-9d,
            +2.0958481826069642E-8d,
            +8.96162975425619E-8d,
            -3.535214171178576E-8d,
            -1.1455271549574576E-7d,
            +9.140964977432485E-8d,
            +1.0667524445105459E-7d,
            -6.777752790396222E-8d,
            +4.586785041291296E-8d,
            -2.8245462428022094E-8d,
            -5.071761314397018E-8d,
            -2.0566368810068663E-8d,
            +6.319146317890346E-8d,
            -3.687854305539139E-8d,
            -8.137269363160008E-8d,
            -6.930491127388755E-8d,
            +3.1184473002226595E-10d,
            -1.0995299963140049E-7d,
            +7.772668425499348E-8d,
            +8.750367485925089E-8d,
            -7.963112393823186E-8d,
            +5.415131809829094E-8d,
            +1.3006683896462346E-8d,
            +3.634736373360733E-8d,
            -1.132504393233074E-7d,
            +4.2046187038837375E-8d,
            +2.6396811618001066E-8d,
            +7.92177143584738E-8d,
            -3.691100820545433E-8d,
            -8.257112559083188E-8d,
            -5.676200971739166E-8d,
            +4.151794514828518E-8d,
            -2.5147255753587636E-8d,
            -1.7335469415174996E-8d,
            +6.595784859136531E-8d,
            -1.2680354928109105E-8d,
            -1.3824992526093461E-8d,
            +6.353142754175797E-8d,
            -1.8021197722549054E-8d,
            -1.9054827792903468E-8d,
            +6.144098503892116E-8d,
            -1.3940903373095247E-8d,
            -5.7694907599522404E-9d,
            +8.696863522320578E-8d,
            +2.6869297963554945E-8d,
            +5.3366470162689076E-8d,
            -7.094204160127543E-8d,
            -1.0662027949814858E-7d,
            -5.26498707801063E-8d,
            +9.198855229106814E-8d,
            +8.989677431456647E-8d,
            -5.790384407322479E-8d,
            -1.1197236522467887E-7d,
            -7.12854317090566E-8d,
            +6.51813137650059E-8d,
            +6.003465022483798E-8d,
            -8.569906238528267E-8d,
            +1.0584469687624562E-7d,
            -7.956144278281947E-8d,
            +7.43676272093501E-8d,
            +9.182512565315022E-8d,
            -2.6157563728873715E-8d,
            -4.012947040998503E-8d,
            +5.094280572218447E-8d,
            +9.675095351161728E-9d,
            +7.552139802281006E-8d,
            +1.1099566726533146E-8d,
            +5.58656252899437E-8d,
            -2.756054703800197E-8d,
            +2.791018095971047E-10d,
            -9.799351869734466E-8d,
            -8.291832428736212E-8d,
            +4.654720780112994E-8d,
            +5.302803981406403E-8d,
            -6.243126731995636E-8d,
            -6.036655299348577E-8d,
            +6.026878587378257E-8d,
            +6.210379583313526E-8d,
            -5.381287389094251E-8d,
            -4.8012970400697E-8d,
            +8.055420567281602E-8d,
            +9.452180117175641E-8d,
            -5.057430382371206E-9d,
            +2.1288872215266507E-8d,
            -6.380305844689076E-8d,
            -2.0858800984600168E-8d,
            -8.724006061713588E-8d,
            -2.3470351753125604E-8d,
            -6.690931338790221E-8d,
            +2.192160831263035E-8d,
            +5.6648446166177225E-9d,
            -1.1461755745719884E-7d,
            -9.944393412663547E-8d,
            +5.2249837964645906E-8d,
            +1.0311034276196487E-7d,
            +5.4203784018566126E-8d,
            -9.340259278913173E-8d,
            -1.0022192034216903E-7d,
            +3.481513333662908E-8d,
            +7.436036590244714E-8d,
            +1.9485199912395296E-8d,
            +1.0968068384729757E-7d,
            +1.0760175582979094E-7d,
            +1.4322981952798675E-8d,
            +6.933855730431659E-8d,
            +3.530656968851287E-8d,
            -8.669526204279467E-8d,
            -5.7169586962345785E-8d,
            -1.1345515834332824E-7d,
            -1.605251622332555E-8d,
            -2.298302779758532E-9d,
            -7.110952399338234E-8d,
            +1.70164513845372E-8d,
            +2.4746155561368937E-8d,
            -4.6834239957353325E-8d,
            +4.1781076667923185E-8d,
            +5.326182134294869E-8d,
            -1.1302647617762544E-8d,
            +8.759667154796094E-8d,
            +1.126326877851684E-7d,
            +6.48979555673987E-8d,
            -5.451390316294111E-8d,
            -6.0896188500539086E-9d,
            -2.7152010585461855E-8d,
            -1.1660424775832058E-7d,
            -3.492984900939992E-8d,
            -1.944841848873016E-8d,
            -6.905990750285027E-8d,
            +5.575538653428039E-8d,
            +1.1768108384670781E-7d,
            +1.178204606523101E-7d,
            +5.727787111340131E-8d,
            -6.284125161007433E-8d,
            -3.0118152047565877E-9d,
            -5.448044533034374E-10d,
            -5.433154287341921E-8d,
            +7.515630833946181E-8d,
            -8.780756503572527E-8d,
            -6.527407547535494E-8d,
            -9.45487863616303E-8d,
            +6.390098458668406E-8d,
            -6.564672913105876E-8d,
            -5.238488022920792E-9d,
            +7.824500749252316E-9d,
            -2.5339299158309795E-8d,
            -1.036103313062145E-7d,
            +1.2550633697348567E-8d,
            +8.584676196065558E-8d,
            +1.1740089468291563E-7d,
            +1.0833697012353316E-7d,
            +5.978002467397905E-8d,
            -2.7143806069290897E-8d,
            +8.711129287069315E-8d,
            -7.316349947981893E-8d,
            -3.00015852582934E-8d,
            -2.0691000399732483E-8d,
            -4.4100097152254264E-8d,
            -9.909612209943178E-8d,
            +5.38733640215475E-8d,
            -6.0893829005035E-8d,
            +3.457553391989844E-8d,
            +1.0300006058273187E-7d,
            -9.290053015365092E-8d,
            -7.514966995961323E-8d,
            -8.10254145615142E-8d,
            -1.0938612624777085E-7d,
            +7.932952721989251E-8d,
            +9.428257290008738E-9d,
            -7.952636967837795E-8d,
            +5.203033137154554E-8d,
            -7.159157201731446E-8d,
            +2.7593424989059015E-8d,
            +1.1231621190000476E-7d,
            -5.469119869891027E-8d,
            +4.560067256086347E-9d,
            +5.280427179595944E-8d,
            +9.119538242455128E-8d,
            -1.1753008498403413E-7d,
            -9.537874867759656E-8d,
            -7.96118345325538E-8d,
            -6.907085854395348E-8d,
            -6.259620482221904E-8d,
            -5.902712448725381E-8d,
            -5.720173456146447E-8d,
            -5.5957016861703E-8d,
            -5.412881689012608E-8d,
            -5.0551842723970724E-8d,
            -4.405966390424518E-8d,
            -3.348471032333413E-8d,
            -1.7658271111516935E-8d,
            +4.589506477601956E-9d,
            +3.4429618182751655E-8d,
            +7.303420385174346E-8d,
            -1.168420305422519E-7d,
            -5.718749537552229E-8d,
            +1.4754809136835937E-8d,
            +1.001616104682875E-7d,
            -3.8207793300052055E-8d,
            +7.766278405014509E-8d,
            -2.7883635712109803E-8d,
            -1.1524714043067699E-7d,
            +5.517333625963128E-8d,
            +7.724278756071081E-9d,
            -1.7990934773848504E-8d,
            -2.0786347668702902E-8d,
            +5.251554594269693E-10d,
            +4.7131849857076246E-8d,
            -1.1819540733893871E-7d,
            -1.742885956093543E-8d,
            +1.1220467571570283E-7d,
            +3.347954541376715E-8d,
            -1.399157980498908E-8d,
            -2.9013441705763093E-8d,
            -1.0389614239253089E-8d,
            +4.307749759934266E-8d,
            -1.0583192018912101E-7d,
            +2.0919226941745448E-8d,
            -5.2305110482722706E-8d,
            -8.588407110184028E-8d,
            -7.861419797923639E-8d,
            -2.929085835358592E-8d,
            +6.329175751021792E-8d,
            -3.807794163054899E-8d,
            -9.377320954068088E-8d,
            -1.0258469865953145E-7d,
            -6.330187984612758E-8d,
            +2.5286958775281306E-8d,
            -7.40238661307607E-8d,
            +1.1681688445204168E-7d,
            -1.1623125976292733E-7d,
            -5.6696107089038004E-8d,
            +5.822140627806124E-8d,
            -8.678466172071259E-9d,
            -1.7757121899175995E-8d,
            +3.220665454652531E-8d,
            -9.598330731102836E-8d,
            +7.573375369829243E-8d,
            +7.174547784678893E-8d,
            -1.0672213971363184E-7d,
            +1.8395252217743006E-8d,
            -2.8511112548600118E-8d,
            -7.79306270997787E-9d,
            +8.178019529487065E-8d,
            +3.0220784595602374E-9d,
            -4.4156343103298585E-9d,
            +6.07014616741277E-8d,
            -3.8809601937571554E-8d,
            -6.329342805230603E-8d,
            -1.1511990258493999E-8d,
            +1.177739474561431E-7d,
            +8.738625278484571E-8d,
            -1.0143341551207646E-7d,
            +2.9394972678456236E-8d,
            +4.278345398213486E-9d,
            +6.28805835150457E-8d,
            -3.197037359731606E-8d,
            -4.060821046423735E-8d,
            +3.82160283750664E-8d,
            -3.2666060441373307E-8d,
            -1.3584500601329896E-8d,
            +9.671332777035621E-8d,
            +6.10626893063691E-8d,
            +1.1913723189736356E-7d,
            +3.3774671482641995E-8d,
            +4.4651109654500895E-8d,
            -8.539328154875224E-8d,
            -1.166799420361101E-7d,
            -4.794765976694151E-8d,
            -1.1635256954820579E-7d,
            -8.221241452580445E-8d,
            +5.5737717715868425E-8d,
            +6.034539636024073E-8d,
            -6.712199323081945E-8d,
            -8.697724830833087E-8d,
            +2.0494942705297694E-9d,
            -3.718924074653624E-8d,
            +3.499747150995707E-8d,
            -1.8535359161566028E-8d,
            +4.1905679587096103E-8d,
            -2.0821912536551675E-8d,
            +3.297776915751238E-8d,
            -3.3835280846270374E-8d,
            +1.8437339356553904E-8d,
            -4.734187609526424E-8d,
            +8.527976799299225E-9d,
            -5.1088103279787804E-8d,
            +1.3513294656751725E-8d,
            -3.480032127343472E-8d,
            +4.367697180842916E-8d,
            +1.1815196363705356E-8d,
            +1.0932279207149782E-7d,
            +9.907230065250944E-8d,
            -1.764389559496152E-8d,
            -1.1135725625095859E-9d,
            -8.846040040259342E-8d,
            -3.996962588736431E-8d,
            -9.276238757878814E-8d,
            -7.12139818505956E-9d,
            -2.016525972830718E-8d,
            +1.0782585410141121E-7d,
            -9.868269632073771E-8d,
            +7.686861750031585E-8d,
            -7.947087669425045E-8d,
            -8.955768055535647E-8d,
            +4.791582240886607E-8d,
            +9.583994718167641E-8d,
            +5.5524866689108584E-8d,
            -7.171796605211277E-8d,
            -4.6157237582310713E-8d,
            -1.0489751005162237E-7d,
            -8.204903560604627E-9d,
            +6.818588687884566E-9d,
            -5.850916105103205E-8d,
            +3.5549586192569994E-8d,
            +5.1896700056778354E-8d,
            -8.146080588190463E-9d,
            +9.516285362051742E-8d,
            -1.1368933260611668E-7d,
            +8.187871486648885E-8d,
            -3.206182925646474E-8d,
            +2.265440168347286E-8d,
            +8.938334752179552E-9d,
            -7.187922490287331E-8d,
            +1.9952407216533937E-8d,
            +4.734805892507655E-8d,
            +1.1642439930208906E-8d,
            -8.582843599651953E-8d,
            -5.3086706437795354E-9d,
            +1.6121782610217253E-8d,
            -2.0197142620980974E-8d,
            -1.129242035557684E-7d,
            -2.2298267863810133E-8d,
            +1.4605950309628873E-8d,
            -8.663710700190489E-10d,
            -6.736873974532501E-8d,
            +5.486523121881414E-8d,
            -1.0965249168570443E-7d,
            -8.27343074126263E-8d,
            -1.0144703278439455E-7d,
            +7.39809943048038E-8d,
            -3.193297932837415E-8d,
            +5.900393284617182E-8d,
            +1.0973020465397083E-7d,
            -1.1681436418514489E-7d,
            +9.5985669644661E-8d,
            +3.423560333632085E-8d,
            -6.22836197265283E-8d,
            +4.621027492345726E-8d,
            -1.1575484316683829E-7d,
            -6.997545435826076E-8d,
            -5.3502441327259514E-8d,
            -6.49667713553005E-8d,
            -1.029980741248172E-7d,
            +7.219393868923887E-8d,
            -1.4854841678687828E-8d,
            +1.1406713393562271E-7d,
            -1.650155887561251E-8d,
            +7.165331603232264E-8d,
            -9.692697614257269E-8d,
            -4.402550702194912E-8d,
            -6.679737442193143E-9d,
            +1.6492800268960003E-8d,
            +2.68759245092879E-8d,
            +2.5854805721793077E-8d,
            +1.4815967715704613E-8d,
            -4.852711011229633E-9d,
            -3.176199594915881E-8d,
            -6.452129525125173E-8d,
            -1.01738658407525E-7d,
            +9.639780418418697E-8d,
            +5.4445606140746644E-8d,
            +1.2219361033150988E-8d,
            -2.8883532688356087E-8d,
            -6.746431126005811E-8d,
            -1.0212284427080097E-7d,
            +1.0696094577483825E-7d,
            +8.43527683868743E-8d,
            +6.987544103716777E-8d,
            +6.493457409236137E-8d,
            +7.093715125593688E-8d,
            +8.929153091001965E-8d,
            -1.1701113164306871E-7d,
            -6.972256643013266E-8d,
            -5.848862070736576E-9d,
            +7.602385197610123E-8d,
            -6.110775144284437E-8d,
            +6.101012058093429E-8d,
            -3.304167134225169E-8d,
            -1.0342514383702196E-7d,
            +8.969907328603505E-8d,
            +7.091600108064668E-8d,
            +8.006778743052707E-8d,
            +1.1857939200074815E-7d,
            -5.0541412403312774E-8d,
            +5.0970277930552287E-8d,
            -5.229355472795119E-8d,
            +1.1793478462381443E-7d,
            +8.625007227318527E-8d,
            +9.250422086873268E-8d,
            -1.0028661472061573E-7d,
            -1.384914052949463E-8d,
            +1.1483560326413004E-7d,
            +4.878798101459259E-8d,
            +2.7866921183936055E-8d,
            +5.3514180410849046E-8d,
            -1.1124565511436785E-7d,
            +1.186914813275767E-8d,
            -5.253258132241335E-8d,
            -6.458486486369316E-8d,
            -2.2838888809969377E-8d,
            +7.415557606805398E-8d,
            -1.0568403170659571E-8d,
            -3.7139182948393606E-8d,
            -4.1022790876160215E-9d,
            +8.999821367768787E-8d,
            +8.201043988912348E-9d,
            -9.616457442665051E-9d,
            +3.8005886250603055E-8d,
            -8.588890051473289E-8d,
            +9.699937202692456E-8d,
            +1.11298006674538E-7d,
            -4.1527104733570825E-8d,
            +1.1682852007826251E-7d,
            +1.1099648061301941E-7d,
            -5.755303038890997E-8d,
            +8.948877445235827E-8d,
            +7.675780395028194E-8d,
            -9.427143563390596E-8d,
            +5.471416081500162E-8d,
            +4.8354824064383506E-8d,
            -1.118706134478866E-7d,
            +5.235528379688445E-8d,
            +6.567708120053687E-8d,
            -7.042204992948526E-8d,
            -1.1603891006723397E-7d,
            -6.968742825553785E-8d,
            +7.01199184127881E-8d,
            +6.645352711199266E-8d,
            -7.919617109348822E-8d,
            +1.1149986927391714E-7d,
            -7.522074418324674E-8d,
            +7.739252980388984E-8d,
            +9.39987974788905E-8d,
            -2.390421480210064E-8d,
            -3.639873824357815E-8d,
            +5.8015881615938497E-8d,
            +2.2423186335040668E-8d,
            +9.674534330665206E-8d,
            +4.4068830785712375E-8d,
            +1.0431875573076199E-7d,
            +4.0584538834428926E-8d,
            +9.279423236781974E-8d,
            +2.404020521381534E-8d,
            +7.425346071427343E-8d,
            +6.529321706138789E-9d,
            +6.080174837146273E-8d,
            +1.6902327633329284E-10d,
            +6.456806922371733E-8d,
            +1.7100134295216033E-8d,
            +9.770510970673519E-8d,
            +6.94872148530716E-8d,
            -6.602926393514549E-8d,
            -6.889997193778161E-8d,
            +6.240235720677117E-8d,
            +9.098790295810902E-8d,
            +1.8386917534879182E-8d,
            +8.454972737414241E-8d,
            +5.259099728747365E-8d,
            -7.595453077213505E-8d,
            -6.113203624663034E-8d,
            +9.859622328905143E-8d,
            -7.206766550807255E-8d,
            -9.474579567171831E-8d,
            +3.210408693366267E-8d,
            +7.160716418525417E-8d,
            +2.530870537724554E-8d,
            -1.0524451040704701E-7d,
            -8.008561371849434E-8d,
            +1.0233519853128553E-7d,
            -3.326791455362767E-8d,
            -8.504961764629757E-9d,
            -6.024017201863256E-8d,
            +5.1500902632092514E-8d,
            +8.98570720774568E-8d,
            +5.638724693948384E-8d,
            -4.734813904255994E-8d,
            +1.8631451577542948E-8d,
            +1.7470924137873214E-8d,
            -4.926470933588261E-8d,
            +5.84096713620797E-8d,
            +1.0364355880696472E-7d,
            +8.800655674349468E-8d,
            +1.3069802481237792E-8d,
            +1.1882454749452428E-7d,
            -6.999215748398631E-8d,
            -7.49674072510849E-8d,
            +1.054760847603618E-7d,
            -3.920012014371067E-9d,
            +7.526183084319617E-8d,
            +1.0618494853096868E-7d,
            +9.043280094115832E-8d,
            +2.9590395068826316E-8d,
            -7.475571347653619E-8d,
            +1.7401160143611842E-8d,
            +6.923209420670962E-8d,
            +8.232829924979753E-8d,
            +5.82825404854514E-8d,
            -1.3108606792380822E-9d,
            -9.485602512220194E-8d,
            +1.7663064617118723E-8d,
            +9.942682855652123E-8d,
            -8.638275100090915E-8d,
            -6.132639063569726E-8d,
            -6.221897889344726E-8d,
            -8.745525834919404E-8d,
            +1.029901759234897E-7d,
            +3.3888561478632076E-8d,
            -5.47315553588771E-8d,
            +7.715994473741065E-8d,
            -4.566098167230033E-8d,
            +5.5257514455273825E-8d,
            -9.530545662611411E-8d,
            -1.889488909834863E-8d,
            +4.769006625301079E-8d,
            +1.0607041998938709E-7d,
            -8.054981263802322E-8d,
            -3.370929373457322E-8d,
            +9.799164177397836E-9d,
            +5.160291611526656E-8d,
            +9.333090708652975E-8d,
            -1.0180490545927503E-7d,
            -5.533523366931846E-8d,
            -4.044932340334176E-9d,
            +5.370131904567218E-8d,
            -1.1887814032213867E-7d,
            -4.3307634616102625E-8d,
            +4.363437558318513E-8d,
            -9.482896784430338E-8d,
            +1.9782818312325887E-8d,
            -8.77224935488516E-8d,
            +6.113879253864931E-8d,
            -8.822335132515693E-9d,
            -5.753754066078771E-8d,
            -8.335545536862392E-8d,
            -8.462309712606694E-8d,
            -5.968586877433824E-8d,
            -6.887556547891059E-9d,
            +7.542967150507818E-8d,
            -4.949331199790077E-8d,
            +9.684172421525468E-8d,
            +3.9260317944365246E-8d,
            +1.784536881359796E-8d,
            +3.426282345243592E-8d,
            +9.018025618601154E-8d,
            -5.1151708476133135E-8d,
            +8.877492215808044E-8d,
            +3.479545684576179E-8d,
            +2.7002575714977818E-8d,
            +6.707201545505014E-8d,
            -8.173742908533777E-8d,
            +5.909041310777802E-8d,
            +1.439903710393587E-8d,
            +2.4289317341982113E-8d,
            +9.044519282818302E-8d,
            -2.3866331257845713E-8d,
            -7.853944465095286E-8d,
            -7.188526769607005E-8d,
            -2.2132706360079843E-9d,
            -1.0624985110080394E-7d,
            +9.453598391231829E-8d,
            -1.134160131581847E-7d,
            -1.315295870404327E-8d,
            -7.981320644583728E-8d,
            -7.327771300038971E-8d,
            +8.155647334672472E-9d,
            -7.222791579580787E-8d,
            -7.430436987497092E-8d,
            +3.633404807819848E-9d,
            -7.512438321498593E-8d,
            -7.044869765481105E-8d,
            +1.9372589859580955E-8d,
            -4.2365298585101096E-8d,
            -1.552830824758035E-8d,
            +1.0160071259930585E-7d,
            +7.232201430620959E-8d,
            -1.0164389431039905E-7d,
            +5.826233477413577E-8d,
            +7.6927415825689E-8d,
            -4.392309439525734E-8d,
            -6.414337408955734E-8d,
            +1.799550702470095E-8d,
            -3.4194410638967946E-8d,
            +1.9437762419688045E-8d,
            -5.7792549966531335E-8d,
            -2.5731071572354522E-8d,
            +1.173595905705643E-7d,
            -1.0361863127101014E-7d,
            +2.8330789837569332E-8d,
            +3.81131861433539E-8d,
            -7.252724942149532E-8d,
            -6.342604067787756E-8d,
            +6.716441526213986E-8d,
            +8.257484966196574E-8d,
            -1.5443717968117592E-8d,
            +1.3280021798948244E-8d,
            -6.79180673261558E-8d,
            -1.8863249269709046E-8d,
            -7.62162303263991E-8d,
            +2.011589233663723E-10d,
            -2.62683511147141E-8d,
            +8.455684903712996E-8d,
            +9.602293320384794E-8d,
            +9.896378545255258E-9d,
            +6.636396724067746E-8d,
            +2.8777050870552646E-8d,
            -1.0109271059094341E-7d,
            -8.305334708631055E-8d,
            +8.467026501338835E-8d,
            -7.29821745001452E-8d,
            -7.739491336852633E-8d,
            +7.321238022013781E-8d,
            -9.621538067089515E-8d,
            -1.0705722541811197E-7d,
            +4.247240125405735E-8d,
            +1.1574222007764044E-7d,
            +1.145412771487496E-7d,
            +4.066036653218687E-8d,
            -1.0410796803072171E-7d,
            -7.955085231106037E-8d,
            +1.1612776191572459E-7d,
            +7.888519481107568E-9d,
            +7.436813814737735E-8d,
            +7.894935661289349E-8d,
            +2.343525263620692E-8d,
            -9.036933434595339E-8d,
            -2.2239222395888823E-8d,
            -8.784622656707742E-9d,
            -4.819540032304379E-8d,
            +9.975892708522332E-8d,
            -3.9945124955316294E-8d,
            +1.1345047468988893E-8d,
            +1.702808472925844E-8d,
            -2.10770182066344E-8d,
            -1.0114948914089626E-7d,
            +1.70518021921727E-8d,
            +9.693260855961159E-8d,
            -9.809953482725758E-8d,
            -8.937957126662392E-8d,
            -1.134963954323427E-7d,
            +6.980004387880031E-8d,
            -1.4494150014095534E-8d,
            +1.122932337832262E-7d,
            -2.483811732227808E-8d,
            +5.278759515330048E-8d,
            +1.0859222881334994E-7d,
            -9.400056055939758E-8d,
            -7.630957994128623E-8d,
            -7.490757191850264E-8d,
            -8.794689652049879E-8d,
            -1.1357810855950775E-7d,
            +8.846862323478745E-8d,
            +4.32092015744956E-8d,
            -9.082923009890997E-9d,
            -6.655106680680314E-8d,
            +1.1108184705020206E-7d,
            +4.8838973948592766E-8d,
            -1.2998975819628988E-8d,
            -7.25680516883106E-8d,
            -1.280024819379844E-7d,
            -1.7743467191652895E-7d,
            -2.1899520225809197E-7d,
            +2.2602433110285232E-7d,
            +2.0582268590356215E-7d,
            +1.9911192455808124E-7d,
            +2.0776878313278689E-7d,
            +2.3367183133931002E-7d,
            -1.9813568387704588E-7d,
            -1.320972037315105E-7d,
            -4.316580502355056E-8d,
            +7.054443447243064E-8d,
            +2.109212796025238E-7d,
            -9.698281856949837E-8d,
            +1.0239791185239086E-7d,
            -1.4271754202157014E-7d,
            +1.232402895636637E-7d,
            -5.150590480969644E-8d,
            -1.882201085012735E-7d,
            +1.918355503889933E-7d,
            +1.368893262241355E-7d,
            +1.256828068633383E-7d,
            +1.601222826656464E-7d,
            -2.3472125169205568E-7d,
            -1.032634625827871E-7d,
            +7.957037517331382E-8d,
            -1.6114314525832115E-7d,
            +1.3018591370778052E-7d,
            +1.8007284821359149E-9d,
            -6.75421764491544E-8d,
            -7.592155950645605E-8d,
            -2.1414301981236817E-8d,
            +9.79045937979623E-8d,
            -1.9287515190177685E-7d,
            +6.184953843236509E-8d,
            -8.966500602352001E-8d,
            -1.686490951669855E-7d,
            -1.7316830893872364E-7d,
            -1.0128633727463388E-7d,
            +4.8935021740786486E-8d,
            -1.9740129448026905E-7d,
            +1.1532102163380318E-7d,
            +3.5371542244169364E-8d,
            +4.153321337726989E-8d,
            +1.3575372396796738E-7d,
            -1.5685449228299222E-7d,
            +1.1933437776279623E-7d,
            +1.2599421120614435E-8d,
            +1.7331079674066365E-9d,
            +8.869266069401045E-8d,
            -2.013999442282902E-7d,
            +8.709065843311144E-8d,
            +2.453117120472083E-9d,
            +2.3489472779602617E-8d,
            +1.5216652792122652E-7d,
            -8.638415150333099E-8d,
            -2.1335475961524608E-7d,
            -2.2677272333821516E-7d,
            -1.246635423141374E-7d,
            +9.494921297991565E-8d,
            -4.27932550865546E-8d,
            -5.907349480138712E-8d,
            +4.809072216941908E-8d,
            -1.9615359732789476E-7d,
            +1.6385396676990034E-7d,
            +1.7642714221524228E-7d,
            -1.564440844355254E-7d,
            +1.2090653407564583E-7d,
            +5.679855838941285E-8d,
            +1.3006497185242537E-7d,
            -1.341336085949317E-7d,
            +2.1987686050231372E-7d,
            -2.3641341460419062E-7d,
            -7.048932272279454E-8d,
            -2.3401958604540354E-7d,
            +2.2867766559333004E-7d,
            -1.1089952719756529E-7d,
            +1.7977178878541792E-7d,
            +1.4903074102418675E-7d,
            -2.011072593789072E-7d,
            +8.504948422097802E-8d,
            +5.5846006716348844E-8d,
            +1.9014079059505456E-7d,
            +1.3119976852347583E-8d,
            +3.645999732952202E-9d,
            +1.6374611405314333E-7d,
            +1.8612397134087598E-8d,
            +4.7113225346448296E-8d,
            -2.2555535676499395E-7d,
            +1.5631615647329739E-7d,
            -2.3574653182047758E-7d,
            +3.08072210937242E-8d,
            +4.344259288116142E-9d,
            +1.6374489573868447E-7d,
            +3.42171232580676E-8d,
            +9.46452492584643E-8d,
            -1.297587351085525E-7d,
            -1.601065201853145E-7d,
            +5.6550495386976275E-9d,
            -1.0725602261510391E-7d,
            -1.9945408945084193E-8d,
            -2.071910882200156E-7d,
            -1.900947109027913E-7d,
            +3.34069282059055E-8d,
            -1.145810806477298E-8d,
            +1.5421457732308477E-7d,
            +5.5657084775121975E-8d,
            +1.7177785285061278E-7d,
            +2.7813027425289027E-8d,
            +1.0267509648109748E-7d,
            -7.839574072711142E-8d,
            -3.648293887796095E-8d,
            +2.3049492079013518E-7d,
            -2.290530257391564E-7d,
            +1.747018414872141E-8d,
            +1.8477759656842807E-8d,
            -2.2394073401050633E-7d,
            -2.3085653185818848E-7d,
            -1.7598351175286083E-10d,
            -6.640551220774385E-9d,
            +2.2868466674913266E-7d,
            +2.3106230530437902E-7d,
            +2.594209135294356E-9d,
            +2.2221434720602702E-8d,
            -1.847872222755186E-7d,
            -1.3948659218254467E-7d,
            +1.6023339607737848E-7d,
            -2.3718944120137026E-7d,
            +1.0087056692827474E-7d,
            +2.228553660510707E-7d,
            +1.3088328582956644E-7d,
            -1.7292527438195104E-7d,
            -2.0961068531216087E-7d,
            +2.2951597845188004E-8d,
            +5.005103745740068E-8d,
            -1.2618366811281002E-7d,
            -2.6784582477238417E-8d,
            -1.2645600379949252E-7d,
            +5.3774170051560117E-8d,
            +3.9205810725333715E-8d,
            -1.6802196396307013E-7d,
            -8.893078799284047E-8d,
            -1.9821451970481713E-7d,
            -1.689060694498032E-8d,
            -1.9648717830943396E-8d,
            -2.0433926409457167E-7d,
            -9.1973399031975E-8d,
            -1.5723449006087263E-7d,
            +7.887051614592191E-8d,
            +1.4166246290402286E-7d,
            +3.330146018487787E-8d,
            +2.3278688667580978E-7d,
            -2.1139124097042925E-7d,
            +1.334449995534113E-7d,
            -1.6104730195920897E-7d,
            -1.3902314592614197E-7d,
            +2.0169027167169864E-7d,
            -9.040643863751471E-8d,
            -5.946190852360168E-8d,
            -1.8013411720005014E-7d,
            +2.6595401669835947E-8d,
            +8.607292924069425E-8d,
            +4.84038176769263E-10d,
            -2.2798356346688802E-7d,
            -1.203028719549339E-7d,
            -1.5111906039270745E-7d,
            +1.5859915617670956E-7d,
            -1.426262681506497E-7d,
            -9.892260062323546E-8d,
            -1.8492643515928268E-7d,
            +7.840210076743552E-8d,
            +2.1643071541578027E-7d,
            +2.313664294893465E-7d,
            +1.2541842003811723E-7d,
            -9.920197743470107E-8d,
            +3.655589133934081E-8d,
            +5.807052689551411E-8d,
            -3.244024724169575E-8d,
            -2.327564406466327E-7d,
            -6.38187356721971E-8d,
            -2.3995994000400915E-10d,
            -3.9793609609721186E-8d,
            -1.802510054588344E-7d,
            +5.745586744591196E-8d,
            +1.987228872666507E-7d,
            -2.3105188606976847E-7d,
            +2.0088042407239129E-7d,
            +6.624793114025702E-8d,
            -1.5587043044056635E-7d,
            +1.3606464059428694E-8d,
            +1.0008761540741556E-7d,
            +1.058213771597129E-7d,
            +3.3058299602856804E-8d,
            -1.1594886810010702E-7d,
            +1.378919824418909E-7d,
            -1.5683631181406778E-7d,
            -4.4200075770425176E-8d,
            +1.2250985436706623E-9d,
            -1.8297013058336644E-8d,
            -1.005004229646318E-7d,
            +2.337202285991116E-7d,
            +3.296104292035678E-8d,
            -2.23668185816307E-7d,
            -5.7055442971184756E-8d,
            +5.82391923137467E-8d,
            +1.244950238958056E-7d,
            +1.4399358260219398E-7d,
            +1.1901862840583523E-7d,
            +5.1856152603337505E-8d,
            -5.520562000491495E-8d,
            -1.9987622893254038E-7d,
            +9.697418238031897E-8d,
            -1.1603376405901542E-7d,
            +1.170714288147407E-7d,
            -1.550851303094034E-7d,
            +2.3472546699189522E-8d,
            +1.78211222185955E-7d,
            -1.6540009048230807E-7d,
            -5.137865010872577E-8d,
            +4.57490653163866E-8d,
            +1.2829599363166098E-7d,
            +1.985773325073412E-7d,
            -2.1792661654989742E-7d,
            -1.652218131743459E-7d,
            -1.178234251477505E-7d,
            -7.34071933723896E-8d,
            -2.9646587857612632E-8d,
            +1.5787194498912167E-8d,
            +6.52252321321176E-8d,
            +1.2100088103262734E-7d,
            +1.8544977697201776E-7d,
            -2.159273204728711E-7d,
            -1.2711589287782304E-7d,
            -2.2610609958205195E-8d,
            +9.993330547750349E-8d,
            -2.33974236642384E-7d,
            -6.830955860192377E-8d,
            +1.2244183812423448E-7d,
            -1.3620325027706252E-7d,
            +1.1178574689680927E-7d,
            -8.490693031052439E-8d,
            +2.2975389535985893E-7d,
            +1.0445707500867073E-7d,
            +1.8405243253979117E-8d,
            -2.6033812325397097E-8d,
            -2.6489990728664908E-8d,
            +1.9409124727247465E-8d,
            +1.1403826867020365E-7d,
            -2.1706266226554237E-7d,
            -1.7839974359909697E-8d,
            +2.3725087624341041E-7d,
            +7.37567604176979E-8d,
            -2.9098805266958403E-8d,
            -6.892713087722722E-8d,
            -4.333719263537725E-8d,
            +5.006436936098099E-8d,
            +2.1367325342138113E-7d,
            -2.6949659655907758E-8d,
            -1.9256682968755803E-7d,
            +1.960616287777496E-7d,
            +1.876664741413704E-7d,
            -2.1534486893602122E-7d,
            -5.688830723853217E-8d,
            +1.8861113228746644E-7d,
            +4.6730779443102234E-8d,
            -3.275360514112964E-9d,
            +4.1011920825226876E-8d,
            +1.820141955326842E-7d,
            -5.468175655175594E-8d,
            -1.8981247089866317E-7d,
            -2.209492705846306E-7d,
            -1.4566110577298295E-7d,
            +3.848544860465368E-8d,
            -1.429109630340783E-7d,
            -2.105749999899302E-7d,
            -1.6206609756618993E-7d,
            +5.058693461947143E-9d,
            -1.8359244902596882E-7d,
            +2.2810251664891242E-7d,
            -1.8791776732592608E-7d,
            +1.3106843166204263E-9d,
            -1.5543153797220025E-7d,
            -1.7884997059081524E-7d,
            -6.648490725635754E-8d,
            +1.8412576154421806E-7d,
            +9.860939269906055E-8d,
            +1.5627006743114285E-7d,
            -1.17260039161597E-7d,
            +2.3416513526430908E-7d,
            -2.1749172296989992E-7d,
            -3.9242560971295217E-8d,
            -1.822826971477839E-7d,
            -1.6729355321895212E-7d,
            +8.208715337901827E-9d,
            -1.301267783434537E-7d,
            -1.029741755377153E-7d,
            +9.215765583599035E-8d,
            -1.907487641016455E-8d,
            +4.2661388254716074E-8d,
            -1.9697226735187428E-7d,
            +2.1819935527247946E-7d,
            -1.398318929248588E-7d,
            +1.6195123407015624E-7d,
            +1.723826394935661E-7d,
            -1.0602700638269148E-7d,
            -1.9392742205954563E-7d,
            -8.880302882034106E-8d,
            +2.1186420987133E-7d,
            +2.3375763256988976E-7d,
            -2.0599801342241997E-8d,
            -7.184550924856607E-8d,
            +8.254840070367875E-8d,
    };

    private static final double[][] LN_MANT = new double[][]{
            {+0.0d, +0.0d,}, // 0
            {+9.760860120877624E-4d, -3.903230345984362E-11d,}, // 1
            {+0.0019512202125042677d, -8.124251825289188E-11d,}, // 2
            {+0.0029254043474793434d, -1.8374207360194882E-11d,}, // 3
            {+0.0038986406289041042d, -2.1324678121885073E-10d,}, // 4
            {+0.004870930686593056d, -4.5199654318611534E-10d,}, // 5
            {+0.00584227591753006d, -2.933016992001806E-10d,}, // 6
            {+0.006812678650021553d, -2.325147219074669E-10d,}, // 7
            {+0.007782140746712685d, -3.046577356838847E-10d,}, // 8
            {+0.008750664070248604d, -5.500631513861575E-10d,}, // 9
            {+0.00971824862062931d, +8.48292035519895E-10d,}, // 10
            {+0.010684899985790253d, +1.1422610134013436E-10d,}, // 11
            {+0.01165061630308628d, +9.168889933128375E-10d,}, // 12
            {+0.012615403160452843d, -5.303786078838E-10d,}, // 13
            {+0.013579258695244789d, -5.688639355498786E-10d,}, // 14
            {+0.01454218477010727d, +7.296670293275653E-10d,}, // 15
            {+0.015504186972975731d, -4.370104767451421E-10d,}, // 16
            {+0.016465261578559875d, +1.43695591408832E-9d,}, // 17
            {+0.01742541790008545d, -1.1862263158849434E-9d,}, // 18
            {+0.018384650349617004d, -9.482976524690715E-10d,}, // 19
            {+0.01934296265244484d, +1.9068609515836638E-10d,}, // 20
            {+0.020300358533859253d, +2.655990315697216E-10d,}, // 21
            {+0.021256837993860245d, +1.0315548713040775E-9d,}, // 22
            {+0.022212404757738113d, +5.13345647019085E-10d,}, // 23
            {+0.02316705882549286d, +4.5604151934208014E-10d,}, // 24
            {+0.02412080392241478d, -1.1255706987475148E-9d,}, // 25
            {+0.025073636323213577d, +1.2289023836765196E-9d,}, // 26
            {+0.02602556347846985d, +1.7990281828096504E-9d,}, // 27
            {+0.026976589113473892d, -1.4152718164638451E-9d,}, // 28
            {+0.02792670577764511d, +7.568772963781632E-10d,}, // 29
            {+0.0288759246468544d, -1.1449998592111558E-9d,}, // 30
            {+0.029824241995811462d, -1.6850976862319495E-9d,}, // 31
            {+0.030771657824516296d, +8.422373919843096E-10d,}, // 32
            {+0.0317181795835495d, +6.872350402175489E-10d,}, // 33
            {+0.03266380727291107d, -4.541194749189272E-10d,}, // 34
            {+0.03360854089260101d, -8.9064764856495E-10d,}, // 35
            {+0.034552380442619324d, +1.0640404096769032E-9d,}, // 36
            {+0.0354953333735466d, -3.5901655945224663E-10d,}, // 37
            {+0.03643739968538284d, -3.4829517943661266E-9d,}, // 38
            {+0.037378571927547455d, +8.149473794244232E-10d,}, // 39
            {+0.03831886500120163d, -6.990650304449166E-10d,}, // 40
            {+0.03925827145576477d, +1.0883076226453258E-9d,}, // 41
            {+0.040196798741817474d, +3.845192807999274E-10d,}, // 42
            {+0.04113444685935974d, -1.1570594692045927E-9d,}, // 43
            {+0.04207121580839157d, -1.8877045166697178E-9d,}, // 44
            {+0.043007105588912964d, -1.6332083257987747E-10d,}, // 45
            {+0.04394212365150452d, -1.7950057534514933E-9d,}, // 46
            {+0.04487626254558563d, +2.302710041648838E-9d,}, // 47
            {+0.045809537172317505d, -1.1410233017161343E-9d,}, // 48
            {+0.04674194008111954d, -3.0498741599744685E-9d,}, // 49
            {+0.04767347127199173d, -1.8026348269183678E-9d,}, // 50
            {+0.04860413819551468d, -3.233204600453039E-9d,}, // 51
            {+0.04953393340110779d, +1.7211688427961583E-9d,}, // 52
            {+0.05046287178993225d, -2.329967807055457E-10d,}, // 53
            {+0.05139094591140747d, -4.191810118556531E-11d,}, // 54
            {+0.052318163216114044d, -3.5574324788328143E-9d,}, // 55
            {+0.053244516253471375d, -1.7346590916458485E-9d,}, // 56
            {+0.05417001247406006d, -4.343048751383674E-10d,}, // 57
            {+0.055094651877880096d, +1.92909364037955E-9d,}, // 58
            {+0.056018441915512085d, -5.139745677199588E-10d,}, // 59
            {+0.05694137513637543d, +1.2637629975129189E-9d,}, // 60
            {+0.05786345899105072d, +1.3840561112481119E-9d,}, // 61
            {+0.058784693479537964d, +1.414889689612056E-9d,}, // 62
            {+0.05970507860183716d, +2.9199191907666474E-9d,}, // 63
            {+0.0606246218085289d, +7.90594243412116E-12d,}, // 64
            {+0.06154331564903259d, +1.6844747839686189E-9d,}, // 65
            {+0.06246116757392883d, +2.0498074572151747E-9d,}, // 66
            {+0.06337818503379822d, -4.800180493433863E-9d,}, // 67
            {+0.06429435312747955d, -2.4220822960064277E-9d,}, // 68
            {+0.06520968675613403d, -4.179048566709334E-9d,}, // 69
            {+0.06612417101860046d, +6.363872957010456E-9d,}, // 70
            {+0.06703783571720123d, +9.339468680056365E-10d,}, // 71
            {+0.06795066595077515d, -4.04226739708981E-9d,}, // 72
            {+0.0688626617193222d, -7.043545052852817E-9d,}, // 73
            {+0.06977382302284241d, -6.552819560439773E-9d,}, // 74
            {+0.07068414986133575d, -1.0571674860370546E-9d,}, // 75
            {+0.07159365713596344d, -3.948954622015801E-9d,}, // 76
            {+0.07250232994556427d, +1.1776625988228244E-9d,}, // 77
            {+0.07341018319129944d, +9.221072639606492E-10d,}, // 78
            {+0.07431721687316895d, -3.219119568928366E-9d,}, // 79
            {+0.0752234160900116d, +5.147575929018918E-9d,}, // 80
            {+0.07612881064414978d, -2.291749683541979E-9d,}, // 81
            {+0.07703337073326111d, +5.749565906124772E-9d,}, // 82
            {+0.07793712615966797d, +9.495158151301779E-10d,}, // 83
            {+0.07884006202220917d, -3.144331429489291E-10d,}, // 84
            {+0.0797421783208847d, +3.430029236134205E-9d,}, // 85
            {+0.08064348995685577d, -1.2499290483167703E-9d,}, // 86
            {+0.08154398202896118d, +2.011215719133196E-9d,}, // 87
            {+0.08244366943836212d, -2.2728753031387152E-10d,}, // 88
            {+0.0833425521850586d, -6.508966857277253E-9d,}, // 89
            {+0.0842406153678894d, -4.801131671405377E-10d,}, // 90
            {+0.08513787388801575d, +4.406750291994231E-9d,}, // 91
            {+0.08603434264659882d, -5.304795662536171E-9d,}, // 92
            {+0.08692999184131622d, +1.6284313912612293E-9d,}, // 93
            {+0.08782485127449036d, -3.158898981674071E-9d,}, // 94
            {+0.08871890604496002d, -3.3324878834139977E-9d,}, // 95
            {+0.08961215615272522d, +2.536961912893389E-9d,}, // 96
            {+0.09050461649894714d, +9.737596728980696E-10d,}, // 97
            {+0.0913962870836258d, -6.600437262505396E-9d,}, // 98
            {+0.09228715300559998d, -3.866609889222889E-9d,}, // 99
            {+0.09317722916603088d, -4.311847594020281E-9d,}, // 100
            {+0.09406651556491852d, -6.525851105645959E-9d,}, // 101
            {+0.09495499730110168d, +5.799080912675435E-9d,}, // 102
            {+0.09584270417690277d, +4.2634204358490415E-9d,}, // 103
            {+0.09672962129116058d, +5.167390528799477E-9d,}, // 104
            {+0.09761576354503632d, -4.994827392841906E-9d,}, // 105
            {+0.09850110113620758d, +4.970725577861395E-9d,}, // 106
            {+0.09938566386699677d, +6.6496705953229645E-9d,}, // 107
            {+0.10026945173740387d, +1.4262712796792241E-9d,}, // 108
            {+0.1011524498462677d, +5.5822855204629114E-9d,}, // 109
            {+0.10203467309474945d, +5.593494835247651E-9d,}, // 110
            {+0.10291612148284912d, +2.8332008343480686E-9d,}, // 111
            {+0.10379679501056671d, -1.3289231465997192E-9d,}, // 112
            {+0.10467669367790222d, -5.526819276639527E-9d,}, // 113
            {+0.10555580258369446d, +6.503128678219282E-9d,}, // 114
            {+0.10643415153026581d, +6.317463237641817E-9d,}, // 115
            {+0.10731174051761627d, -4.728528221305482E-9d,}, // 116
            {+0.10818853974342346d, +4.519199083083901E-9d,}, // 117
            {+0.10906457901000977d, +5.606492666349878E-9d,}, // 118
            {+0.10993985831737518d, -1.220176214398581E-10d,}, // 119
            {+0.11081436276435852d, +3.5759315936869937E-9d,}, // 120
            {+0.11168810725212097d, +3.1367659571899855E-9d,}, // 121
            {+0.11256109178066254d, -1.0543075713098835E-10d,}, // 122
            {+0.11343331634998322d, -4.820065619207094E-9d,}, // 123
            {+0.11430476605892181d, +5.221136819669415E-9d,}, // 124
            {+0.11517547070980072d, +1.5395018670011342E-9d,}, // 125
            {+0.11604541540145874d, +3.5638391501880846E-10d,}, // 126
            {+0.11691460013389587d, +2.9885336757136527E-9d,}, // 127
            {+0.11778303980827332d, -4.151889860890893E-9d,}, // 128
            {+0.11865071952342987d, -4.853823938804204E-9d,}, // 129
            {+0.11951763927936554d, +2.189226237170704E-9d,}, // 130
            {+0.12038381397724152d, +3.3791993048776982E-9d,}, // 131
            {+0.1212492436170578d, +1.5811884868243975E-11d,}, // 132
            {+0.12211392819881439d, -6.6045909118908625E-9d,}, // 133
            {+0.1229778528213501d, -2.8786263916116364E-10d,}, // 134
            {+0.12384103238582611d, +5.354472503748251E-9d,}, // 135
            {+0.12470348179340363d, -3.2924463896248744E-9d,}, // 136
            {+0.12556517124176025d, +4.856678149580005E-9d,}, // 137
            {+0.12642613053321838d, +1.2791850600366742E-9d,}, // 138
            {+0.12728634476661682d, +2.1525945093362843E-9d,}, // 139
            {+0.12814581394195557d, +8.749974471767862E-9d,}, // 140
            {+0.129004567861557d, -7.461209161105275E-9d,}, // 141
            {+0.12986254692077637d, +1.4390208226263824E-8d,}, // 142
            {+0.1307198405265808d, -1.3839477920475328E-8d,}, // 143
            {+0.13157635927200317d, -1.483283901239408E-9d,}, // 144
            {+0.13243216276168823d, -6.889072914229094E-9d,}, // 145
            {+0.1332872211933136d, +9.990351100568362E-10d,}, // 146
            {+0.13414156436920166d, -6.370937412495338E-9d,}, // 147
            {+0.13499516248703003d, +2.05047480130511E-9d,}, // 148
            {+0.1358480453491211d, -2.29509872547079E-9d,}, // 149
            {+0.13670018315315247d, +1.16354361977249E-8d,}, // 150
            {+0.13755163550376892d, -1.452496267904829E-8d,}, // 151
            {+0.1384023129940033d, +9.865115839786888E-9d,}, // 152
            {+0.13925230503082275d, -3.369999130712228E-9d,}, // 153
            {+0.14010155200958252d, +6.602496401651853E-9d,}, // 154
            {+0.14095008373260498d, +1.1205312852298845E-8d,}, // 155
            {+0.14179790019989014d, +1.1660367213160203E-8d,}, // 156
            {+0.142645001411438d, +9.186471222585239E-9d,}, // 157
            {+0.14349138736724854d, +4.999341878263704E-9d,}, // 158
            {+0.14433705806732178d, +3.11611905696257E-10d,}, // 159
            {+0.14518201351165771d, -3.6671598175618173E-9d,}, // 160
            {+0.14602625370025635d, -5.730477881659618E-9d,}, // 161
            {+0.14686977863311768d, -4.674900007989718E-9d,}, // 162
            {+0.1477125883102417d, +6.999732437141968E-10d,}, // 163
            {+0.14855468273162842d, +1.159150872494107E-8d,}, // 164
            {+0.14939609169960022d, -6.082714828488485E-10d,}, // 165
            {+0.15023678541183472d, -4.905712741596318E-9d,}, // 166
            {+0.1510767638683319d, -1.124848988733307E-10d,}, // 167
            {+0.15191605687141418d, -1.484557220949851E-8d,}, // 168
            {+0.15275460481643677d, +1.1682026251371384E-8d,}, // 169
            {+0.15359249711036682d, -8.757272519238786E-9d,}, // 170
            {+0.15442964434623718d, +1.4419920764774415E-8d,}, // 171
            {+0.15526613593101501d, -7.019891063126053E-9d,}, // 172
            {+0.15610191226005554d, -1.230153548825964E-8d,}, // 173
            {+0.15693697333335876d, -2.574172005933276E-10d,}, // 174
            {+0.15777134895324707d, +4.748140799544371E-10d,}, // 175
            {+0.15860503911972046d, -8.943081874891003E-9d,}, // 176
            {+0.15943801403045654d, +2.4500739038517657E-9d,}, // 177
            {+0.1602703034877777d, +6.007922084557054E-9d,}, // 178
            {+0.16110190749168396d, +2.8835418231126645E-9d,}, // 179
            {+0.1619328260421753d, -5.772862039728412E-9d,}, // 180
            {+0.16276302933692932d, +1.0988372954605789E-8d,}, // 181
            {+0.16359257698059082d, -5.292913162607026E-9d,}, // 182
            {+0.16442140936851501d, +6.12956339275823E-9d,}, // 183
            {+0.16524958610534668d, -1.3210039516811888E-8d,}, // 184
            {+0.16607704758644104d, -2.5711014608334873E-9d,}, // 185
            {+0.16690382361412048d, +9.37721319457112E-9d,}, // 186
            {+0.1677299439907074d, -6.0370682395944045E-9d,}, // 187
            {+0.168555349111557d, +1.1918249660105651E-8d,}, // 188
            {+0.1693800985813141d, +4.763282949656017E-9d,}, // 189
            {+0.17020416259765625d, +3.4223342273948817E-9d,}, // 190
            {+0.1710275411605835d, +9.014612241310916E-9d,}, // 191
            {+0.1718502640724182d, -7.145758990550526E-9d,}, // 192
            {+0.172672301530838d, -1.4142763934081504E-8d,}, // 193
            {+0.1734936535358429d, -1.0865453656579032E-8d,}, // 194
            {+0.17431432008743286d, +3.794385569450774E-9d,}, // 195
            {+0.1751343309879303d, +1.1399188501627291E-9d,}, // 196
            {+0.17595365643501282d, +1.2076238768270153E-8d,}, // 197
            {+0.1767723262310028d, +7.901084730502162E-9d,}, // 198
            {+0.17759034037590027d, -1.0288181007465474E-8d,}, // 199
            {+0.1784076690673828d, -1.15945645153806E-8d,}, // 200
            {+0.17922431230545044d, +5.073923825786778E-9d,}, // 201
            {+0.18004029989242554d, +1.1004278077575267E-8d,}, // 202
            {+0.1808556318283081d, +7.2831502374676964E-9d,}, // 203
            {+0.18167030811309814d, -5.0054634662706464E-9d,}, // 204
            {+0.18248429894447327d, +5.022108460298934E-9d,}, // 205
            {+0.18329763412475586d, +8.642254225732676E-9d,}, // 206
            {+0.18411031365394592d, +6.931054493326395E-9d,}, // 207
            {+0.18492233753204346d, +9.619685356326533E-10d,}, // 208
            {+0.18573370575904846d, -8.194157257980706E-9d,}, // 209
            {+0.18654438853263855d, +1.0333241479437797E-8d,}, // 210
            {+0.1873544454574585d, -1.9948340196027965E-9d,}, // 211
            {+0.1881638467311859d, -1.4313002926259948E-8d,}, // 212
            {+0.1889725625514984d, +4.241536392174967E-9d,}, // 213
            {+0.18978065252304077d, -4.877952454011428E-9d,}, // 214
            {+0.1905880868434906d, -1.0813801247641613E-8d,}, // 215
            {+0.1913948655128479d, -1.2513218445781325E-8d,}, // 216
            {+0.19220098853111267d, -8.925958555729115E-9d,}, // 217
            {+0.1930064558982849d, +9.956860681280245E-10d,}, // 218
            {+0.193811297416687d, -1.1505428993246996E-8d,}, // 219
            {+0.1946154534816742d, +1.4217997464522202E-8d,}, // 220
            {+0.19541901350021362d, -1.0200858727747717E-8d,}, // 221
            {+0.19622188806533813d, +5.682607223902455E-9d,}, // 222
            {+0.1970241367816925d, +3.2988908516009827E-9d,}, // 223
            {+0.19782572984695435d, +1.3482965534659446E-8d,}, // 224
            {+0.19862669706344604d, +7.462678536479685E-9d,}, // 225
            {+0.1994270384311676d, -1.3734273888891115E-8d,}, // 226
            {+0.20022669434547424d, +1.0521983802642893E-8d,}, // 227
            {+0.20102575421333313d, -8.152742388541905E-9d,}, // 228
            {+0.2018241584300995d, -9.133484280193855E-9d,}, // 229
            {+0.20262190699577332d, +8.59763959528144E-9d,}, // 230
            {+0.2034190595149994d, -1.3548568223001414E-8d,}, // 231
            {+0.20421552658081055d, +1.4847880344628818E-8d,}, // 232
            {+0.20501139760017395d, +5.390620378060543E-9d,}, // 233
            {+0.2058066427707672d, -1.1109834472051523E-8d,}, // 234
            {+0.20660123229026794d, -3.845373872038116E-9d,}, // 235
            {+0.20739519596099854d, -1.6149279479975042E-9d,}, // 236
            {+0.20818853378295898d, -3.4174925203771133E-9d,}, // 237
            {+0.2089812457561493d, -8.254443919468538E-9d,}, // 238
            {+0.20977330207824707d, +1.4672790944499144E-8d,}, // 239
            {+0.2105647623538971d, +6.753452542942992E-9d,}, // 240
            {+0.21135559678077698d, -1.218609462241927E-9d,}, // 241
            {+0.21214580535888672d, -8.254218316367887E-9d,}, // 242
            {+0.21293538808822632d, -1.3366540360587255E-8d,}, // 243
            {+0.2137243151664734d, +1.4231244750190031E-8d,}, // 244
            {+0.2145126760005951d, -1.3885660525939072E-8d,}, // 245
            {+0.21530038118362427d, -7.3304404046850136E-9d,}, // 246
            {+0.2160874605178833d, +5.072117654842356E-9d,}, // 247
            {+0.21687394380569458d, -5.505080220459036E-9d,}, // 248
            {+0.21765980124473572d, -8.286782292266659E-9d,}, // 249
            {+0.2184450328350067d, -2.302351152358085E-9d,}, // 250
            {+0.21922963857650757d, +1.3416565858314603E-8d,}, // 251
            {+0.22001364827156067d, +1.0033721426962048E-8d,}, // 252
            {+0.22079706192016602d, -1.1487079818684332E-8d,}, // 253
            {+0.22157981991767883d, +9.420348186357043E-9d,}, // 254
            {+0.2223619818687439d, +1.4110645699377834E-8d,}, // 255
            {+0.2231435477733612d, +3.5408485497116107E-9d,}, // 256
            {+0.22392448782920837d, +8.468072777056227E-9d,}, // 257
            {+0.2247048318386078d, +4.255446699237779E-11d,}, // 258
            {+0.22548454999923706d, +9.016946273084244E-9d,}, // 259
            {+0.22626367211341858d, +6.537034810260226E-9d,}, // 260
            {+0.22704219818115234d, -6.451285264969768E-9d,}, // 261
            {+0.22782009840011597d, +7.979956357126066E-10d,}, // 262
            {+0.22859740257263184d, -5.759582672039005E-10d,}, // 263
            {+0.22937411069869995d, -9.633854121180397E-9d,}, // 264
            {+0.23015019297599792d, +4.363736368635843E-9d,}, // 265
            {+0.23092567920684814d, +1.2549416560182509E-8d,}, // 266
            {+0.231700599193573d, -1.3946383592553814E-8d,}, // 267
            {+0.2324748933315277d, -1.458843364504023E-8d,}, // 268
            {+0.23324856162071228d, +1.1551692104697154E-8d,}, // 269
            {+0.23402166366577148d, +5.795621295524984E-9d,}, // 270
            {+0.23479416966438293d, -1.1301979046684263E-9d,}, // 271
            {+0.23556607961654663d, -8.303779721781787E-9d,}, // 272
            {+0.23633739352226257d, -1.4805271785394075E-8d,}, // 273
            {+0.23710808157920837d, +1.0085373835899469E-8d,}, // 274
            {+0.2378782033920288d, +7.679117635349454E-9d,}, // 275
            {+0.2386477291584015d, +8.69177352065934E-9d,}, // 276
            {+0.23941665887832642d, +1.4034725764547136E-8d,}, // 277
            {+0.24018502235412598d, -5.185064518887831E-9d,}, // 278
            {+0.2409527599811554d, +1.1544236628121676E-8d,}, // 279
            {+0.24171993136405945d, +5.523085719902123E-9d,}, // 280
            {+0.24248650670051575d, +7.456824943331887E-9d,}, // 281
            {+0.24325251579284668d, -1.1555923403029638E-8d,}, // 282
            {+0.24401789903640747d, +8.988361382732908E-9d,}, // 283
            {+0.2447827160358429d, +1.0381848020926893E-8d,}, // 284
            {+0.24554696679115295d, -6.480706118857055E-9d,}, // 285
            {+0.24631062150001526d, -1.0904271124793968E-8d,}, // 286
            {+0.2470736801624298d, -1.998183061531611E-9d,}, // 287
            {+0.247836172580719d, -8.676137737360023E-9d,}, // 288
            {+0.24859806895256042d, -2.4921733203932487E-10d,}, // 289
            {+0.2493593990802765d, -5.635173762130303E-9d,}, // 290
            {+0.2501201629638672d, -2.3951455355985637E-8d,}, // 291
            {+0.25088030099868774d, +5.287121672447825E-9d,}, // 292
            {+0.2516399025917053d, -6.447877375049486E-9d,}, // 293
            {+0.25239890813827515d, +1.32472428796441E-9d,}, // 294
            {+0.2531573176383972d, +2.9479464287605006E-8d,}, // 295
            {+0.2539151906967163d, +1.9284247135543574E-8d,}, // 296
            {+0.2546725273132324d, -2.8390360197221716E-8d,}, // 297
            {+0.255429208278656d, +6.533522495226226E-9d,}, // 298
            {+0.2561853528022766d, +5.713225978895991E-9d,}, // 299
            {+0.25694090127944946d, +2.9618050962556135E-8d,}, // 300
            {+0.25769591331481934d, +1.950605015323617E-8d,}, // 301
            {+0.25845038890838623d, -2.3762031507525576E-8d,}, // 302
            {+0.2592042088508606d, +1.98818938195077E-8d,}, // 303
            {+0.25995755195617676d, -2.751925069084042E-8d,}, // 304
            {+0.2607102394104004d, +1.3703391844683932E-8d,}, // 305
            {+0.26146239042282104d, +2.5193525310038174E-8d,}, // 306
            {+0.2622140049934387d, +7.802219817310385E-9d,}, // 307
            {+0.26296502351760864d, +2.1983272709242607E-8d,}, // 308
            {+0.2637155055999756d, +8.979279989292184E-9d,}, // 309
            {+0.2644653916358948d, +2.9240221157844312E-8d,}, // 310
            {+0.265214741230011d, +2.4004885823813374E-8d,}, // 311
            {+0.2659635543823242d, -5.885186277410878E-9d,}, // 312
            {+0.2667117714881897d, +1.4300386517357162E-11d,}, // 313
            {+0.2674594521522522d, -1.7063531531989365E-8d,}, // 314
            {+0.26820653676986694d, +3.3218524692903896E-9d,}, // 315
            {+0.2689530849456787d, +2.3998252479954764E-9d,}, // 316
            {+0.2696990966796875d, -1.8997462070389404E-8d,}, // 317
            {+0.27044451236724854d, -4.350745270980051E-10d,}, // 318
            {+0.2711893916130066d, -6.892221115467135E-10d,}, // 319
            {+0.27193373441696167d, -1.89333199110902E-8d,}, // 320
            {+0.272677481174469d, +5.262017392507765E-9d,}, // 321
            {+0.27342069149017334d, +1.3115046679980076E-8d,}, // 322
            {+0.2741633653640747d, +5.4468361834451975E-9d,}, // 323
            {+0.2749055027961731d, -1.692337384653611E-8d,}, // 324
            {+0.27564704418182373d, +6.426479056697412E-9d,}, // 325
            {+0.2763880491256714d, +1.670735065191342E-8d,}, // 326
            {+0.27712851762771606d, +1.4733029698334834E-8d,}, // 327
            {+0.27786844968795776d, +1.315498542514467E-9d,}, // 328
            {+0.2786078453063965d, -2.2735061539223372E-8d,}, // 329
            {+0.27934664487838745d, +2.994379757313727E-9d,}, // 330
            {+0.28008490800857544d, +1.970577274107218E-8d,}, // 331
            {+0.28082263469696045d, +2.820392733542077E-8d,}, // 332
            {+0.2815598249435425d, +2.929187356678173E-8d,}, // 333
            {+0.28229647874832153d, +2.377086680926386E-8d,}, // 334
            {+0.2830325961112976d, +1.2440393009992529E-8d,}, // 335
            {+0.2837681770324707d, -3.901826104778096E-9d,}, // 336
            {+0.2845032215118408d, -2.4459827842685974E-8d,}, // 337
            {+0.2852376699447632d, +1.1165241398059789E-8d,}, // 338
            {+0.28597164154052734d, -1.54434478239181E-8d,}, // 339
            {+0.28670501708984375d, +1.5714110564653245E-8d,}, // 340
            {+0.28743791580200195d, -1.3782394940142479E-8d,}, // 341
            {+0.2881702184677124d, +1.6063569876284005E-8d,}, // 342
            {+0.28890204429626465d, -1.317176818216125E-8d,}, // 343
            {+0.28963327407836914d, +1.8504673536253893E-8d,}, // 344
            {+0.29036402702331543d, -7.334319635123628E-9d,}, // 345
            {+0.29109418392181396d, +2.9300903540317107E-8d,}, // 346
            {+0.2918238639831543d, +9.979706999541057E-9d,}, // 347
            {+0.29255300760269165d, -4.916314210412424E-9d,}, // 348
            {+0.293281614780426d, -1.4611908070155308E-8d,}, // 349
            {+0.2940096855163574d, -1.833351586679361E-8d,}, // 350
            {+0.29473721981048584d, -1.530926726615185E-8d,}, // 351
            {+0.2954642176628113d, -4.7689754029101934E-9d,}, // 352
            {+0.29619067907333374d, +1.4055868011423819E-8d,}, // 353
            {+0.296916663646698d, -1.7672547212604003E-8d,}, // 354
            {+0.2976420521736145d, +2.0020234215759705E-8d,}, // 355
            {+0.2983669638633728d, +8.688424478730524E-9d,}, // 356
            {+0.2990913391113281d, +8.69851089918337E-9d,}, // 357
            {+0.29981517791748047d, +2.0810681643102672E-8d,}, // 358
            {+0.3005385398864746d, -1.3821169493779352E-8d,}, // 359
            {+0.301261305809021d, +2.4769140784919128E-8d,}, // 360
            {+0.3019835948944092d, +1.8127576600610336E-8d,}, // 361
            {+0.3027053475379944d, +2.6612401062437074E-8d,}, // 362
            {+0.3034266233444214d, -8.629042891789934E-9d,}, // 363
            {+0.3041473627090454d, -2.724174869314043E-8d,}, // 364
            {+0.30486756563186646d, -2.8476975783775358E-8d,}, // 365
            {+0.3055872321128845d, -1.1587600174449919E-8d,}, // 366
            {+0.3063063621520996d, +2.417189020581056E-8d,}, // 367
            {+0.3070250153541565d, +1.99407553679345E-8d,}, // 368
            {+0.3077431917190552d, -2.35387025694381E-8d,}, // 369
            {+0.3084607720375061d, +1.3683509995845583E-8d,}, // 370
            {+0.30917787551879883d, +1.3137214081023085E-8d,}, // 371
            {+0.30989450216293335d, -2.444006866174775E-8d,}, // 372
            {+0.3106105327606201d, +2.0896888605749563E-8d,}, // 373
            {+0.31132614612579346d, -2.893149098508887E-8d,}, // 374
            {+0.31204116344451904d, +5.621509038251498E-9d,}, // 375
            {+0.3127557039260864d, +6.0778104626050015E-9d,}, // 376
            {+0.3134697675704956d, -2.6832941696716294E-8d,}, // 377
            {+0.31418323516845703d, +2.6826625274495256E-8d,}, // 378
            {+0.31489628553390503d, -1.1030897183911054E-8d,}, // 379
            {+0.31560879945755005d, -2.047124671392676E-8d,}, // 380
            {+0.3163207769393921d, -7.709990443086711E-10d,}, // 381
            {+0.3170322775840759d, -1.0812918808112342E-8d,}, // 382
            {+0.3177432417869568d, +9.727979174888975E-9d,}, // 383
            {+0.31845372915267944d, +1.9658551724508715E-9d,}, // 384
            {+0.3191636800765991d, +2.6222628001695826E-8d,}, // 385
            {+0.3198731541633606d, +2.3609400272358744E-8d,}, // 386
            {+0.32058215141296387d, -5.159602957634814E-9d,}, // 387
            {+0.32129061222076416d, +2.329701319016099E-10d,}, // 388
            {+0.32199859619140625d, -1.910633190395738E-8d,}, // 389
            {+0.32270604372024536d, -2.863180390093667E-9d,}, // 390
            {+0.32341301441192627d, -9.934041364456825E-9d,}, // 391
            {+0.3241194486618042d, +1.999240777687192E-8d,}, // 392
            {+0.3248254060745239d, +2.801670341647724E-8d,}, // 393
            {+0.32553088665008545d, +1.4842534265191358E-8d,}, // 394
            {+0.32623589038848877d, -1.882789920477354E-8d,}, // 395
            {+0.3269403576850891d, -1.268923579073577E-8d,}, // 396
            {+0.32764434814453125d, -2.564688370677835E-8d,}, // 397
            {+0.3283478021621704d, +2.6015626820520968E-9d,}, // 398
            {+0.32905077934265137d, +1.3147747907784344E-8d,}, // 399
            {+0.3297532796859741d, +6.686493860720675E-9d,}, // 400
            {+0.33045530319213867d, -1.608884086544153E-8d,}, // 401
            {+0.33115679025650024d, +5.118287907840204E-9d,}, // 402
            {+0.3318578004837036d, +1.139367970944884E-8d,}, // 403
            {+0.3325583338737488d, +3.426327822115399E-9d,}, // 404
            {+0.33325839042663574d, -1.809622142990733E-8d,}, // 405
            {+0.3339579105377197d, +7.116780143398601E-9d,}, // 406
            {+0.3346569538116455d, +2.0145352306345386E-8d,}, // 407
            {+0.3353555202484131d, +2.167272474431968E-8d,}, // 408
            {+0.33605360984802246d, +1.2380696294966822E-8d,}, // 409
            {+0.33675122261047363d, -7.050361059209181E-9d,}, // 410
            {+0.3374482989311218d, +2.366314656322868E-8d,}, // 411
            {+0.3381449580192566d, -1.4010540194086646E-8d,}, // 412
            {+0.3388410806655884d, -1.860165465666482E-10d,}, // 413
            {+0.33953672647476196d, +6.206776940880773E-9d,}, // 414
            {+0.34023189544677734d, +5.841137379010982E-9d,}, // 415
            {+0.3409265875816345d, -6.11041311179286E-10d,}, // 416
            {+0.3416208028793335d, -1.2479264502054702E-8d,}, // 417
            {+0.34231454133987427d, -2.909443297645926E-8d,}, // 418
            {+0.34300774335861206d, +9.815805717097634E-9d,}, // 419
            {+0.3437005281448364d, -1.4291517981101049E-8d,}, // 420
            {+0.3443927764892578d, +1.8457821628427503E-8d,}, // 421
            {+0.34508460760116577d, -1.0481908869377813E-8d,}, // 422
            {+0.34577590227127075d, +1.876076001514746E-8d,}, // 423
            {+0.3464667797088623d, -1.2362653723769037E-8d,}, // 424
            {+0.3471571207046509d, +1.6016578405624026E-8d,}, // 425
            {+0.347847044467926d, -1.4652759033760925E-8d,}, // 426
            {+0.3485364317893982d, +1.549533655901835E-8d,}, // 427
            {+0.34922540187835693d, -1.2093068629412478E-8d,}, // 428
            {+0.3499138355255127d, +2.244531711424792E-8d,}, // 429
            {+0.35060185194015503d, +5.538565518604807E-10d,}, // 430
            {+0.35128939151763916d, -1.7511499366215853E-8d,}, // 431
            {+0.3519763946533203d, +2.850385787215544E-8d,}, // 432
            {+0.35266298055648804d, +2.003926370146842E-8d,}, // 433
            {+0.35334908962249756d, +1.734665280502264E-8d,}, // 434
            {+0.3540347218513489d, +2.1071983674869414E-8d,}, // 435
            {+0.35471993684768677d, -2.774475773922311E-8d,}, // 436
            {+0.3554046154022217d, -9.250975291734664E-9d,}, // 437
            {+0.3560888171195984d, +1.7590672330295415E-8d,}, // 438
            {+0.35677260160446167d, -6.1837904549178745E-9d,}, // 439
            {+0.35745590925216675d, -2.0330362973820856E-8d,}, // 440
            {+0.3581387400627136d, -2.42109990366786E-8d,}, // 441
            {+0.3588210940361023d, -1.7188958587407816E-8d,}, // 442
            {+0.35950297117233276d, +1.3711958590112228E-9d,}, // 443
            {+0.3601844310760498d, -2.7501042008405925E-8d,}, // 444
            {+0.36086535453796387d, +1.6036460343275798E-8d,}, // 445
            {+0.3615458607673645d, +1.3405964389498495E-8d,}, // 446
            {+0.36222589015960693d, +2.484237749027735E-8d,}, // 447
            {+0.36290550231933594d, -8.629967484362177E-9d,}, // 448
            {+0.36358463764190674d, -2.6778729562324134E-8d,}, // 449
            {+0.36426329612731934d, -2.8977490516960565E-8d,}, // 450
            {+0.36494147777557373d, -1.4601106624823502E-8d,}, // 451
            {+0.3656191825866699d, +1.69742947894444E-8d,}, // 452
            {+0.3662964701652527d, +6.7666740211281175E-9d,}, // 453
            {+0.36697328090667725d, +1.500201674336832E-8d,}, // 454
            {+0.3676496744155884d, -1.730424167425052E-8d,}, // 455
            {+0.36832553148269653d, +2.9676011119845104E-8d,}, // 456
            {+0.36900103092193604d, -2.2253590346826743E-8d,}, // 457
            {+0.36967599391937256d, +6.3372065441089185E-9d,}, // 458
            {+0.37035053968429565d, -3.145816653215968E-9d,}, // 459
            {+0.37102460861206055d, +9.515812117036965E-9d,}, // 460
            {+0.371698260307312d, -1.4669965113042639E-8d,}, // 461
            {+0.3723714351654053d, -1.548715389333397E-8d,}, // 462
            {+0.37304413318634033d, +7.674361647125109E-9d,}, // 463
            {+0.37371641397476196d, -4.181177882069608E-9d,}, // 464
            {+0.3743882179260254d, +9.158530500130718E-9d,}, // 465
            {+0.3750596046447754d, -1.13047236597869E-8d,}, // 466
            {+0.3757305145263672d, -5.36108186384227E-9d,}, // 467
            {+0.3764009475708008d, +2.7593452284747873E-8d,}, // 468
            {+0.37707096338272095d, +2.8557016344085205E-8d,}, // 469
            {+0.3777405619621277d, -1.868818164036E-9d,}, // 470
            {+0.3784096837043762d, -3.479042513414447E-9d,}, // 471
            {+0.37907832860946655d, +2.432550290565648E-8d,}, // 472
            {+0.37974655628204346d, +2.2538131805476768E-8d,}, // 473
            {+0.38041436672210693d, -8.244395239939089E-9d,}, // 474
            {+0.3810817003250122d, -7.821867597227376E-9d,}, // 475
            {+0.3817485570907593d, +2.4400089062515914E-8d,}, // 476
            {+0.3824149966239929d, +2.9410015940087773E-8d,}, // 477
            {+0.38308101892471313d, +7.799913824734797E-9d,}, // 478
            {+0.38374656438827515d, +1.976524624939355E-8d,}, // 479
            {+0.38441169261932373d, +6.291008309266035E-9d,}, // 480
            {+0.3850763440132141d, +2.757030889767851E-8d,}, // 481
            {+0.38574057817459106d, +2.4585794728405612E-8d,}, // 482
            {+0.3864043951034546d, -2.0764122246389383E-9d,}, // 483
            {+0.3870677351951599d, +7.77328837578952E-9d,}, // 484
            {+0.3877306580543518d, -4.8859560029989374E-9d,}, // 485
            {+0.3883931040763855d, +2.0133131420595028E-8d,}, // 486
            {+0.38905513286590576d, +2.380738071335498E-8d,}, // 487
            {+0.3897167444229126d, +6.7171126157142075E-9d,}, // 488
            {+0.39037787914276123d, +2.9046141593926277E-8d,}, // 489
            {+0.3910386562347412d, -2.7836800219410262E-8d,}, // 490
            {+0.3916988968849182d, +1.545909820981726E-8d,}, // 491
            {+0.39235877990722656d, -1.930436269002062E-8d,}, // 492
            {+0.3930181860923767d, -1.2343297554921835E-8d,}, // 493
            {+0.3936771750450134d, -2.268889128622553E-8d,}, // 494
            {+0.39433568716049194d, +9.835827818608177E-9d,}, // 495
            {+0.39499378204345703d, +2.6197411946856397E-8d,}, // 496
            {+0.3956514596939087d, +2.6965931069318893E-8d,}, // 497
            {+0.3963087201118469d, +1.2710331127772166E-8d,}, // 498
            {+0.39696556329727173d, -1.6001563011916016E-8d,}, // 499
            {+0.39762192964553833d, +1.0016001590267064E-9d,}, // 500
            {+0.3982778787612915d, +4.680767399874334E-9d,}, // 501
            {+0.39893341064453125d, -4.399582029272418E-9d,}, // 502
            {+0.39958852529525757d, -2.5676078228301587E-8d,}, // 503
            {+0.4002431631088257d, +1.0181870233355787E-9d,}, // 504
            {+0.40089738368988037d, +1.6639728835984655E-8d,}, // 505
            {+0.40155118703842163d, +2.174860642202632E-8d,}, // 506
            {+0.40220457315444946d, +1.6903781197123503E-8d,}, // 507
            {+0.40285754203796387d, +2.663119647467697E-9d,}, // 508
            {+0.40351009368896484d, -2.0416603812329616E-8d,}, // 509
            {+0.4041621685028076d, +7.82494078472695E-9d,}, // 510
            {+0.40481382608413696d, +2.833770747113627E-8d,}, // 511
            {+0.40546512603759766d, -1.7929433274271985E-8d,}, // 512
            {+0.40611594915390015d, -1.1214757379328965E-8d,}, // 513
            {+0.4067663550376892d, -1.0571553019207106E-8d,}, // 514
            {+0.40741634368896484d, -1.5449538712332313E-8d,}, // 515
            {+0.40806591510772705d, -2.529950530235105E-8d,}, // 516
            {+0.40871500968933105d, +2.0031331601617008E-8d,}, // 517
            {+0.4093637466430664d, +1.880755298741952E-9d,}, // 518
            {+0.41001206636428833d, -1.9600580584843318E-8d,}, // 519
            {+0.41065990924835205d, +1.573691633515306E-8d,}, // 520
            {+0.4113073945045471d, -1.0772154376548336E-8d,}, // 521
            {+0.411954402923584d, +2.0624330192486066E-8d,}, // 522
            {+0.4126010537147522d, -8.741139170029572E-9d,}, // 523
            {+0.4132472276687622d, +2.0881457123894216E-8d,}, // 524
            {+0.41389304399490356d, -9.177488027521808E-9d,}, // 525
            {+0.4145383834838867d, +2.0829952491625585E-8d,}, // 526
            {+0.4151833653450012d, -7.767915492597301E-9d,}, // 527
            {+0.4158278703689575d, +2.4774753446082082E-8d,}, // 528
            {+0.41647201776504517d, -2.1581119071750435E-10d,}, // 529
            {+0.4171157479286194d, -2.260047972865202E-8d,}, // 530
            {+0.4177590012550354d, +1.775884601423381E-8d,}, // 531
            {+0.41840189695358276d, +2.185301053838889E-9d,}, // 532
            {+0.4190443754196167d, -9.185071463667081E-9d,}, // 533
            {+0.4196864366531372d, -1.5821896727910552E-8d,}, // 534
            {+0.4203280806541443d, -1.719582086188318E-8d,}, // 535
            {+0.42096930742263794d, -1.2778508303324259E-8d,}, // 536
            {+0.42161011695861816d, -2.042639194493364E-9d,}, // 537
            {+0.42225050926208496d, +1.5538093219698803E-8d,}, // 538
            {+0.4228905439376831d, -1.9115659590156936E-8d,}, // 539
            {+0.42353010177612305d, +1.3729680248843432E-8d,}, // 540
            {+0.42416930198669434d, -4.611893838830296E-9d,}, // 541
            {+0.4248080849647522d, -1.4013456880651706E-8d,}, // 542
            {+0.42544645071029663d, -1.3953728897042917E-8d,}, // 543
            {+0.42608439922332764d, -3.912427573594197E-9d,}, // 544
            {+0.4267219305038452d, +1.6629734283189315E-8d,}, // 545
            {+0.42735910415649414d, -1.1413593493354881E-8d,}, // 546
            {+0.42799586057662964d, -2.792046157580119E-8d,}, // 547
            {+0.42863214015960693d, +2.723009182661306E-8d,}, // 548
            {+0.42926812171936035d, -2.4260535621557444E-8d,}, // 549
            {+0.42990362644195557d, -3.064060124024764E-9d,}, // 550
            {+0.43053877353668213d, -2.787640178598121E-8d,}, // 551
            {+0.4311734437942505d, +2.102412085257792E-8d,}, // 552
            {+0.4318077564239502d, +2.4939635093999683E-8d,}, // 553
            {+0.43244171142578125d, -1.5619414792273914E-8d,}, // 554
            {+0.4330751895904541d, +1.9065734894871523E-8d,}, // 555
            {+0.4337083101272583d, +1.0294301092654604E-8d,}, // 556
            {+0.4343410134315491d, +1.8178469851136E-8d,}, // 557
            {+0.4349733591079712d, -1.6379825102473853E-8d,}, // 558
            {+0.4356052279472351d, +2.6334323946685834E-8d,}, // 559
            {+0.43623673915863037d, +2.761628769925529E-8d,}, // 560
            {+0.436867892742157d, -1.2030229087793677E-8d,}, // 561
            {+0.4374985694885254d, +2.7106814809424793E-8d,}, // 562
            {+0.43812888860702515d, +2.631993083235205E-8d,}, // 563
            {+0.43875885009765625d, -1.3890028312254422E-8d,}, // 564
            {+0.43938833475112915d, +2.6186133735555794E-8d,}, // 565
            {+0.4400174617767334d, +2.783809071694788E-8d,}, // 566
            {+0.440646231174469d, -8.436135220472006E-9d,}, // 567
            {+0.44127458333969116d, -2.2534815932619883E-8d,}, // 568
            {+0.4419025182723999d, -1.3961804471714283E-8d,}, // 569
            {+0.4425300359725952d, +1.7778112039716255E-8d,}, // 570
            {+0.4431571960449219d, +1.3574569976673652E-8d,}, // 571
            {+0.4437839984893799d, -2.607907890164073E-8d,}, // 572
            {+0.4444103240966797d, +1.8518879652136628E-8d,}, // 573
            {+0.44503629207611084d, +2.865065604247164E-8d,}, // 574
            {+0.44566190242767334d, +4.806827797299427E-9d,}, // 575
            {+0.4462870955467224d, +7.0816970994232115E-9d,}, // 576
            {+0.44691193103790283d, -2.3640641240074437E-8d,}, // 577
            {+0.4475363492965698d, -2.7267718387865538E-8d,}, // 578
            {+0.4481603503227234d, -3.3126235292976077E-9d,}, // 579
            {+0.4487839937210083d, -1.0894001590268427E-8d,}, // 580
            {+0.4494072198867798d, +1.0077883359971829E-8d,}, // 581
            {+0.4500300884246826d, +4.825712712114668E-10d,}, // 582
            {+0.450652539730072d, +2.0407987470746858E-8d,}, // 583
            {+0.4512746334075928d, +1.073186581170719E-8d,}, // 584
            {+0.4518963694572449d, -2.8064314757880205E-8d,}, // 585
            {+0.45251762866973877d, +2.3709316816226527E-8d,}, // 586
            {+0.4531385898590088d, -1.2281487504266522E-8d,}, // 587
            {+0.4537591338157654d, -1.634864487421458E-8d,}, // 588
            {+0.45437926054000854d, +1.1985747222409522E-8d,}, // 589
            {+0.45499902963638306d, +1.3594057956219485E-8d,}, // 590
            {+0.4556184411048889d, -1.1047585095328619E-8d,}, // 591
            {+0.45623743534088135d, -1.8592937532754405E-9d,}, // 592
            {+0.4568560719490051d, -1.797135137545755E-8d,}, // 593
            {+0.4574742913246155d, +6.943684261645378E-10d,}, // 594
            {+0.4580921530723572d, -4.994175141684681E-9d,}, // 595
            {+0.45870959758758545d, +2.5039391215625133E-8d,}, // 596
            {+0.45932674407958984d, -2.7943366835352838E-8d,}, // 597
            {+0.45994341373443604d, +1.534146910128904E-8d,}, // 598
            {+0.46055978536605835d, -2.3450920230816267E-8d,}, // 599
            {+0.46117573976516724d, -2.4642997069960124E-8d,}, // 600
            {+0.4617912769317627d, +1.2232622070370946E-8d,}, // 601
            {+0.4624064564704895d, +2.80378133047839E-8d,}, // 602
            {+0.46302127838134766d, +2.3238237048117092E-8d,}, // 603
            {+0.46363574266433716d, -1.7013046451109475E-9d,}, // 604
            {+0.46424978971481323d, +1.3287778803035383E-8d,}, // 605
            {+0.46486347913742065d, +9.06393426961373E-9d,}, // 606
            {+0.4654768109321594d, -1.3910598647592876E-8d,}, // 607
            {+0.46608972549438477d, +4.430214458933614E-9d,}, // 608
            {+0.46670228242874146d, +4.942270562885745E-9d,}, // 609
            {+0.4673144817352295d, -1.1914734393460718E-8d,}, // 610
            {+0.4679262638092041d, +1.3922696570638494E-8d,}, // 611
            {+0.46853768825531006d, +2.3307929211781914E-8d,}, // 612
            {+0.46914875507354736d, +1.669813444584674E-8d,}, // 613
            {+0.469759464263916d, -5.450354376430758E-9d,}, // 614
            {+0.47036975622177124d, +1.6922605350647674E-8d,}, // 615
            {+0.4709796905517578d, +2.4667033200046904E-8d,}, // 616
            {+0.47158926725387573d, +1.8236762070433784E-8d,}, // 617
            {+0.472198486328125d, -1.915204563140137E-9d,}, // 618
            {+0.47280728816986084d, +2.426795414605756E-8d,}, // 619
            {+0.4734157919883728d, -2.19717006713618E-8d,}, // 620
            {+0.47402387857437134d, -2.0974352165535873E-8d,}, // 621
            {+0.47463154792785645d, +2.770970558184228E-8d,}, // 622
            {+0.4752389192581177d, +5.32006955298355E-9d,}, // 623
            {+0.47584593296051025d, -2.809054633964104E-8d,}, // 624
            {+0.4764525294303894d, -1.2470243596102937E-8d,}, // 625
            {+0.4770587682723999d, -6.977226702440138E-9d,}, // 626
            {+0.47766464948654175d, -1.1165866833118273E-8d,}, // 627
            {+0.47827017307281494d, -2.4591344661022708E-8d,}, // 628
            {+0.4788752794265747d, +1.2794996377383974E-8d,}, // 629
            {+0.4794800877571106d, -1.7772927065973874E-8d,}, // 630
            {+0.48008447885513306d, +3.35657712457243E-9d,}, // 631
            {+0.48068851232528687d, +1.7020465042442242E-8d,}, // 632
            {+0.481292188167572d, +2.365953779624783E-8d,}, // 633
            {+0.4818955063819885d, +2.3713798664443718E-8d,}, // 634
            {+0.4824984669685364d, +1.7622455019548098E-8d,}, // 635
            {+0.4831010699272156d, +5.823920246566496E-9d,}, // 636
            {+0.4837033152580261d, -1.1244184344361017E-8d,}, // 637
            {+0.48430514335632324d, +2.645961716432205E-8d,}, // 638
            {+0.4849066734313965d, +1.6207809718247905E-10d,}, // 639
            {+0.4855077862739563d, +2.9507744508973654E-8d,}, // 640
            {+0.48610860109329224d, -4.278201128741098E-9d,}, // 641
            {+0.48670899868011475d, +1.844722015961139E-8d,}, // 642
            {+0.4873090982437134d, -2.1092372471088425E-8d,}, // 643
            {+0.4879087805747986d, -3.2555596107382053E-9d,}, // 644
            {+0.48850810527801514d, +1.2784366845429667E-8d,}, // 645
            {+0.48910707235336304d, +2.7457984659996047E-8d,}, // 646
            {+0.48970574140548706d, -1.8409546441412518E-8d,}, // 647
            {+0.49030399322509766d, -5.179903818099661E-9d,}, // 648
            {+0.4909018874168396d, +7.97053127828682E-9d,}, // 649
            {+0.4914994239807129d, +2.146925464473481E-8d,}, // 650
            {+0.4920966625213623d, -2.3861648589988232E-8d,}, // 651
            {+0.4926934838294983d, -8.386923035320549E-9d,}, // 652
            {+0.4932899475097656d, +8.713990131749256E-9d,}, // 653
            {+0.4938860535621643d, +2.7865534085810115E-8d,}, // 654
            {+0.4944818615913391d, -1.011325138560159E-8d,}, // 655
            {+0.4950772523880005d, +1.4409851026316708E-8d,}, // 656
            {+0.495672345161438d, -1.735227547472004E-8d,}, // 657
            {+0.49626702070236206d, +1.4231078209064581E-8d,}, // 658
            {+0.49686139822006226d, -9.628709342929729E-9d,}, // 659
            {+0.4974554181098938d, -2.8907074856577267E-8d,}, // 660
            {+0.4980490207672119d, +1.6419797090870802E-8d,}, // 661
            {+0.49864232540130615d, +7.561041519403049E-9d,}, // 662
            {+0.49923527240753174d, +4.538983468118194E-9d,}, // 663
            {+0.49982786178588867d, +7.770560657946324E-9d,}, // 664
            {+0.500420093536377d, +1.767197002609876E-8d,}, // 665
            {+0.5010119676589966d, +3.46586694799214E-8d,}, // 666
            {+0.5016034841537476d, +5.914537964556077E-8d,}, // 667
            {+0.5021947622299194d, -2.7663203939320167E-8d,}, // 668
            {+0.5027855634689331d, +1.3064749115929298E-8d,}, // 669
            {+0.5033761262893677d, -5.667682106730711E-8d,}, // 670
            {+0.503966212272644d, +1.9424534974370594E-9d,}, // 671
            {+0.5045560598373413d, -4.908494602153544E-8d,}, // 672
            {+0.5051454305648804d, +2.906989285008994E-8d,}, // 673
            {+0.5057345628738403d, -1.602000800745108E-9d,}, // 674
            {+0.5063233375549316d, -2.148245271118002E-8d,}, // 675
            {+0.5069117546081543d, -3.016329994276181E-8d,}, // 676
            {+0.5074998140335083d, -2.7237099632871992E-8d,}, // 677
            {+0.5080875158309937d, -1.2297127301923986E-8d,}, // 678
            {+0.5086748600006104d, +1.5062624834468093E-8d,}, // 679
            {+0.5092618465423584d, +5.524744954836658E-8d,}, // 680
            {+0.5098485946655273d, -1.054736327333046E-8d,}, // 681
            {+0.5104348659515381d, +5.650063324725722E-8d,}, // 682
            {+0.5110208988189697d, +1.8376017791642605E-8d,}, // 683
            {+0.5116065740585327d, -5.309470636324855E-9d,}, // 684
            {+0.512191891670227d, -1.4154089255217218E-8d,}, // 685
            {+0.5127768516540527d, -7.756800301729815E-9d,}, // 686
            {+0.5133614540100098d, +1.4282730618002001E-8d,}, // 687
            {+0.5139456987380981d, +5.2364136172269755E-8d,}, // 688
            {+0.5145297050476074d, -1.2322940607922115E-8d,}, // 689
            {+0.5151132345199585d, +5.903831350855322E-8d,}, // 690
            {+0.5156965255737305d, +2.8426856726994483E-8d,}, // 691
            {+0.5162794589996338d, +1.544882070711032E-8d,}, // 692
            {+0.5168620347976685d, +2.0500353979930155E-8d,}, // 693
            {+0.5174442529678345d, +4.397691311390564E-8d,}, // 694
            {+0.5180262327194214d, -3.2936025225250634E-8d,}, // 695
            {+0.5186077356338501d, +2.857419553449673E-8d,}, // 696
            {+0.5191890001296997d, -9.51761338269325E-9d,}, // 697
            {+0.5197699069976807d, -2.7609457648450225E-8d,}, // 698
            {+0.520350456237793d, -2.5309316441333305E-8d,}, // 699
            {+0.5209306478500366d, -2.2258513086839407E-9d,}, // 700
            {+0.5215104818344116d, +4.203159541613745E-8d,}, // 701
            {+0.5220900774002075d, -1.1356287358852729E-8d,}, // 702
            {+0.5226693153381348d, -4.279090925831093E-8d,}, // 703
            {+0.5232481956481934d, -5.188364552285819E-8d,}, // 704
            {+0.5238267183303833d, -3.82465458937857E-8d,}, // 705
            {+0.5244048833847046d, -1.4923330530645769E-9d,}, // 706
            {+0.5249826908111572d, +5.8765598932137004E-8d,}, // 707
            {+0.5255602598190308d, +2.3703896609663678E-8d,}, // 708
            {+0.5261374711990356d, +1.2917117341231647E-8d,}, // 709
            {+0.5267143249511719d, +2.6789862192139226E-8d,}, // 710
            {+0.527290940284729d, -5.350322253112414E-8d,}, // 711
            {+0.5278670787811279d, +1.0839714455426386E-8d,}, // 712
            {+0.5284429788589478d, -1.821729591343314E-8d,}, // 713
            {+0.5290185213088989d, -2.1083014672301448E-8d,}, // 714
            {+0.5295937061309814d, +2.623848491704216E-9d,}, // 715
            {+0.5301685333251953d, +5.328392630534142E-8d,}, // 716
            {+0.5307431221008301d, +1.206790586971942E-8d,}, // 717
            {+0.5313173532485962d, -1.4356011804377797E-9d,}, // 718
            {+0.5318912267684937d, +1.3152074173459994E-8d,}, // 719
            {+0.5324647426605225d, +5.6208949382936426E-8d,}, // 720
            {+0.5330380201339722d, +8.90310227565917E-9d,}, // 721
            {+0.5336109399795532d, -9.179458802504127E-9d,}, // 722
            {+0.5341835021972656d, +2.337337845617735E-9d,}, // 723
            {+0.5347557067871094d, +4.3828918300477925E-8d,}, // 724
            {+0.535327672958374d, -3.5392250480081715E-9d,}, // 725
            {+0.53589928150177d, -2.0183663375378704E-8d,}, // 726
            {+0.5364705324172974d, -5.730898606435436E-9d,}, // 727
            {+0.537041425704956d, +4.0191927599879235E-8d,}, // 728
            {+0.5376120805740356d, -1.2522542401353875E-9d,}, // 729
            {+0.5381823778152466d, -1.0482571326594316E-8d,}, // 730
            {+0.5387523174285889d, +1.2871924223480165E-8d,}, // 731
            {+0.539322018623352d, -5.002774317612589E-8d,}, // 732
            {+0.539891242980957d, +3.960668706590162E-8d,}, // 733
            {+0.5404602289199829d, +4.372568630242375E-8d,}, // 734
            {+0.5410289764404297d, -3.730232461206926E-8d,}, // 735
            {+0.5415972471237183d, +3.5309026109857795E-8d,}, // 736
            {+0.5421652793884277d, +2.3508325311148225E-8d,}, // 737
            {+0.5427329540252686d, +4.6871403168921666E-8d,}, // 738
            {+0.5433003902435303d, -1.3445113140270216E-8d,}, // 739
            {+0.5438674688339233d, -3.786663982218041E-8d,}, // 740
            {+0.5444341897964478d, -2.602850370608209E-8d,}, // 741
            {+0.5450005531311035d, +2.2433348713144506E-8d,}, // 742
            {+0.5455666780471802d, -1.1326936872620137E-8d,}, // 743
            {+0.5461324453353882d, -7.737252533211342E-9d,}, // 744
            {+0.5466978549957275d, +3.3564604642699844E-8d,}, // 745
            {+0.5472630262374878d, -6.269066061111782E-9d,}, // 746
            {+0.5478278398513794d, -7.667998948729528E-9d,}, // 747
            {+0.5483922958374023d, +2.9728170818998143E-8d,}, // 748
            {+0.5489565134048462d, -1.2930091396008281E-8d,}, // 749
            {+0.5495203733444214d, -1.607434968107079E-8d,}, // 750
            {+0.5500838756561279d, +2.0653935146671156E-8d,}, // 751
            {+0.5506471395492554d, -2.1596593091833788E-8d,}, // 752
            {+0.5512100458145142d, -2.3259315921149476E-8d,}, // 753
            {+0.5517725944519043d, +1.6022492496522704E-8d,}, // 754
            {+0.5523349046707153d, -2.260433328226171E-8d,}, // 755
            {+0.5528968572616577d, -1.957497997726303E-8d,}, // 756
            {+0.5534584522247314d, +2.5465477111883854E-8d,}, // 757
            {+0.5540198087692261d, -6.33792454933092E-9d,}, // 758
            {+0.554580807685852d, +4.577835263278281E-9d,}, // 759
            {+0.5551414489746094d, +5.856589221771548E-8d,}, // 760
            {+0.5557018518447876d, +3.6769498759522324E-8d,}, // 761
            {+0.5562618970870972d, +5.874989409410614E-8d,}, // 762
            {+0.5568217039108276d, +5.649147309876989E-9d,}, // 763
            {+0.5573811531066895d, -2.9726830960751796E-9d,}, // 764
            {+0.5579402446746826d, +3.323458344853057E-8d,}, // 765
            {+0.5584990978240967d, -4.588749093664028E-9d,}, // 766
            {+0.5590575933456421d, +3.115616594184543E-9d,}, // 767
            {+0.5596157312393188d, +5.6696103838614634E-8d,}, // 768
            {+0.5601736307144165d, +3.7291263280048303E-8d,}, // 769
            {+0.5607312917709351d, -5.4751646725093355E-8d,}, // 770
            {+0.5612884759902954d, +1.9332630743320287E-8d,}, // 771
            {+0.5618454217910767d, +2.147161515775941E-8d,}, // 772
            {+0.5624021291732788d, -4.7989172862560625E-8d,}, // 773
            {+0.5629583597183228d, +4.971378973445109E-8d,}, // 774
            {+0.5635144710540771d, -4.2702997139152675E-8d,}, // 775
            {+0.5640701055526733d, +3.273212962622764E-8d,}, // 776
            {+0.5646255016326904d, +3.79438125545842E-8d,}, // 777
            {+0.5651806592941284d, -2.6725298288329835E-8d,}, // 778
            {+0.5657354593276978d, -4.1723833577410244E-8d,}, // 779
            {+0.5662899017333984d, -6.71028256490915E-9d,}, // 780
            {+0.56684410572052d, -4.055299181908475E-8d,}, // 781
            {+0.567397952079773d, -2.3702295314000405E-8d,}, // 782
            {+0.5679514408111572d, +4.4181618172507453E-8d,}, // 783
            {+0.5685046911239624d, +4.4228706309734985E-8d,}, // 784
            {+0.5690577030181885d, -2.3222346436879016E-8d,}, // 785
            {+0.5696103572845459d, -3.862412756175274E-8d,}, // 786
            {+0.5701626539230347d, -1.6390743801589046E-9d,}, // 787
            {+0.5707147121429443d, -3.1139472791083883E-8d,}, // 788
            {+0.5712664127349854d, -7.579587391156013E-9d,}, // 789
            {+0.5718178749084473d, -4.983281844744412E-8d,}, // 790
            {+0.5723689794540405d, -3.835454246739619E-8d,}, // 791
            {+0.5729197263717651d, +2.7190020372374008E-8d,}, // 792
            {+0.5734702348709106d, +2.7925807446276126E-8d,}, // 793
            {+0.574020504951477d, -3.5813506001861646E-8d,}, // 794
            {+0.5745704174041748d, -4.448550564530588E-8d,}, // 795
            {+0.5751199722290039d, +2.2423840341717488E-9d,}, // 796
            {+0.5756692886352539d, -1.450709904687712E-8d,}, // 797
            {+0.5762182474136353d, +2.4806815282282017E-8d,}, // 798
            {+0.5767669677734375d, +1.3057724436551892E-9d,}, // 799
            {+0.5773153305053711d, +3.4529452510568104E-8d,}, // 800
            {+0.5778634548187256d, +5.598413198183808E-9d,}, // 801
            {+0.5784112215042114d, +3.405124925700107E-8d,}, // 802
            {+0.5789587497711182d, +1.0074354568442952E-9d,}, // 803
            {+0.5795059204101562d, +2.600448597385527E-8d,}, // 804
            {+0.5800528526306152d, -9.83920263200211E-9d,}, // 805
            {+0.5805994272232056d, +1.3012807963586057E-8d,}, // 806
            {+0.5811457633972168d, -2.432215917965441E-8d,}, // 807
            {+0.5816917419433594d, -2.308736892479391E-9d,}, // 808
            {+0.5822374820709229d, -3.983067093146514E-8d,}, // 809
            {+0.5827828645706177d, -1.735366061128156E-8d,}, // 810
            {+0.5833280086517334d, -5.376251584638963E-8d,}, // 811
            {+0.5838727951049805d, -2.952399778965259E-8d,}, // 812
            {+0.5844172239303589d, +5.5685313670430624E-8d,}, // 813
            {+0.5849615335464478d, -3.6230268489088716E-8d,}, // 814
            {+0.5855053663253784d, +5.267948957869391E-8d,}, // 815
            {+0.5860490798950195d, -3.489144132234588E-8d,}, // 816
            {+0.5865923166275024d, +5.9006122320612716E-8d,}, // 817
            {+0.5871354341506958d, -2.2934896740542648E-8d,}, // 818
            {+0.5876781940460205d, -4.1975650319859075E-8d,}, // 819
            {+0.5882205963134766d, +2.2036094805348692E-9d,}, // 820
            {+0.5887627601623535d, -9.287179048539306E-9d,}, // 821
            {+0.5893045663833618d, +4.3079982556221595E-8d,}, // 822
            {+0.589846134185791d, +4.041399585161321E-8d,}, // 823
            {+0.5903874635696411d, -1.696746473863933E-8d,}, // 824
            {+0.5909284353256226d, -9.53795080582038E-9d,}, // 825
            {+0.5914691686630249d, -5.619010749352923E-8d,}, // 826
            {+0.5920095443725586d, -3.7398514182529506E-8d,}, // 827
            {+0.5925495624542236d, +4.71524479659295E-8d,}, // 828
            {+0.5930894613265991d, -4.0640692434639215E-8d,}, // 829
            {+0.5936288833618164d, +5.716453096255401E-8d,}, // 830
            {+0.5941681861877441d, -1.6745661720946737E-8d,}, // 831
            {+0.5947071313858032d, -2.3639110433141897E-8d,}, // 832
            {+0.5952457189559937d, +3.67972590471072E-8d,}, // 833
            {+0.595784068107605d, +4.566672575206695E-8d,}, // 834
            {+0.5963221788406372d, +3.2813537149653483E-9d,}, // 835
            {+0.5968599319458008d, +2.916199305533732E-8d,}, // 836
            {+0.5973974466323853d, +4.410412409109416E-9d,}, // 837
            {+0.5979346036911011d, +4.85464582112459E-8d,}, // 838
            {+0.5984715223312378d, +4.267089756924666E-8d,}, // 839
            {+0.5990082025527954d, -1.2906712010774655E-8d,}, // 840
            {+0.5995445251464844d, +1.3319784467641742E-9d,}, // 841
            {+0.6000806093215942d, -3.35137581974451E-8d,}, // 842
            {+0.6006163358688354d, +2.0734340706476473E-9d,}, // 843
            {+0.6011518239974976d, -1.0808162722402073E-8d,}, // 844
            {+0.601686954498291d, +4.735781872502109E-8d,}, // 845
            {+0.6022218465805054d, +5.76686738430634E-8d,}, // 846
            {+0.6027565002441406d, +2.043049589651736E-8d,}, // 847
            {+0.6032907962799072d, +5.515817703577808E-8d,}, // 848
            {+0.6038248538970947d, +4.2947540692649586E-8d,}, // 849
            {+0.6043586730957031d, -1.589678872195875E-8d,}, // 850
            {+0.6048921346664429d, -1.8613847754677912E-9d,}, // 851
            {+0.6054253578186035d, -3.3851886626187444E-8d,}, // 852
            {+0.6059582233428955d, +7.64416021682279E-9d,}, // 853
            {+0.6064908504486084d, +3.7201467248814224E-9d,}, // 854
            {+0.6070232391357422d, -4.532172996647129E-8d,}, // 855
            {+0.6075552701950073d, -1.997046552871766E-8d,}, // 856
            {+0.6080870628356934d, -3.913411606668587E-8d,}, // 857
            {+0.6086184978485107d, +1.6697361107868944E-8d,}, // 858
            {+0.609149694442749d, +2.8614950293715483E-8d,}, // 859
            {+0.6096806526184082d, -3.081552929643174E-9d,}, // 860
            {+0.6102112531661987d, +4.111645931319645E-8d,}, // 861
            {+0.6107416152954102d, +4.2298539553668435E-8d,}, // 862
            {+0.6112717390060425d, +7.630546413718035E-10d,}, // 863
            {+0.6118015050888062d, +3.601718675118614E-8d,}, // 864
            {+0.6123310327529907d, +2.914906573537692E-8d,}, // 865
            {+0.6128603219985962d, -1.9544361222269494E-8d,}, // 866
            {+0.613389253616333d, +9.442671392695732E-9d,}, // 867
            {+0.6139179468154907d, -2.8031202304593286E-9d,}, // 868
            {+0.6144464015960693d, -5.598619958143586E-8d,}, // 869
            {+0.6149744987487793d, -3.060220883766096E-8d,}, // 870
            {+0.6155023574829102d, -4.556583652800433E-8d,}, // 871
            {+0.6160298585891724d, +1.8626341656366314E-8d,}, // 872
            {+0.6165571212768555d, +4.305870564227991E-8d,}, // 873
            {+0.6170841455459595d, +2.8024460607734262E-8d,}, // 874
            {+0.6176109313964844d, -2.6183651590639875E-8d,}, // 875
            {+0.6181373596191406d, -6.406189112730307E-11d,}, // 876
            {+0.6186635494232178d, -1.2534241706168776E-8d,}, // 877
            {+0.6191893815994263d, +5.5906456251308664E-8d,}, // 878
            {+0.6197150945663452d, -3.286964881802063E-8d,}, // 879
            {+0.6202404499053955d, -4.0153537978961E-8d,}, // 880
            {+0.6207654476165771d, +3.434477109643361E-8d,}, // 881
            {+0.6212903261184692d, -4.750377491075032E-8d,}, // 882
            {+0.6218148469924927d, -4.699152670372743E-8d,}, // 883
            {+0.6223390102386475d, +3.617013128065961E-8d,}, // 884
            {+0.6228630542755127d, -3.6149218175202596E-8d,}, // 885
            {+0.6233867406845093d, -2.5243286814648133E-8d,}, // 886
            {+0.6239101886749268d, -5.003410681432538E-8d,}, // 887
            {+0.6244332790374756d, +8.974417915105033E-9d,}, // 888
            {+0.6249561309814453d, +3.285935446876949E-8d,}, // 889
            {+0.6254787445068359d, +2.190661054038537E-8d,}, // 890
            {+0.6260011196136475d, -2.3598354190515998E-8d,}, // 891
            {+0.6265231370925903d, +1.5838762427747586E-8d,}, // 892
            {+0.6270449161529541d, +2.129323729978037E-8d,}, // 893
            {+0.6275664567947388d, -6.950808333865794E-9d,}, // 894
            {+0.6280876398086548d, +5.059959203156465E-8d,}, // 895
            {+0.6286087036132812d, -4.41909071122557E-8d,}, // 896
            {+0.6291294097900391d, -5.262093550784066E-8d,}, // 897
            {+0.6296497583389282d, +2.559185648444699E-8d,}, // 898
            {+0.6301699876785278d, -4.768920119497491E-8d,}, // 899
            {+0.6306898593902588d, -3.376406008397877E-8d,}, // 900
            {+0.6312094926834106d, -5.156097914033476E-8d,}, // 901
            {+0.6317287683486938d, +1.840992392368355E-8d,}, // 902
            {+0.632247805595398d, +5.721951534729663E-8d,}, // 903
            {+0.6327667236328125d, -5.406177467045421E-8d,}, // 904
            {+0.6332851648330688d, +4.247320713683124E-8d,}, // 905
            {+0.6338034868240356d, -1.0524557502830645E-8d,}, // 906
            {+0.6343214511871338d, +2.5641927558519502E-8d,}, // 907
            {+0.6348391771316528d, +3.204135737993823E-8d,}, // 908
            {+0.6353566646575928d, +8.951285029786536E-9d,}, // 909
            {+0.6358739137649536d, -4.335116707228395E-8d,}, // 910
            {+0.6363908052444458d, -5.380016714089483E-9d,}, // 911
            {+0.6369074583053589d, +3.931710344901743E-9d,}, // 912
            {+0.6374238729476929d, -1.5140150088220166E-8d,}, // 913
            {+0.6379399299621582d, +5.688910024377372E-8d,}, // 914
            {+0.638455867767334d, -1.8124135273572568E-8d,}, // 915
            {+0.6389714479446411d, -1.486720391901626E-9d,}, // 916
            {+0.6394867897033691d, -1.2133811978747018E-8d,}, // 917
            {+0.6400018930435181d, -4.9791700939901716E-8d,}, // 918
            {+0.6405166387557983d, +5.022188652837274E-9d,}, // 919
            {+0.6410311460494995d, +3.337143177933685E-8d,}, // 920
            {+0.6415454149246216d, +3.55284719912458E-8d,}, // 921
            {+0.6420594453811646d, +1.1765332726757802E-8d,}, // 922
            {+0.6425732374191284d, -3.7646381826067834E-8d,}, // 923
            {+0.6430866718292236d, +6.773803682579552E-9d,}, // 924
            {+0.6435998678207397d, +2.608736797081283E-8d,}, // 925
            {+0.6441128253936768d, +2.056466263408266E-8d,}, // 926
            {+0.6446255445480347d, -9.524376551107945E-9d,}, // 927
            {+0.6451379060745239d, +5.5299060775883977E-8d,}, // 928
            {+0.6456501483917236d, -2.3114497793159813E-8d,}, // 929
            {+0.6461620330810547d, -6.077779731902102E-9d,}, // 930
            {+0.6466736793518066d, -1.2531793589140273E-8d,}, // 931
            {+0.6471850872039795d, -4.220866994206517E-8d,}, // 932
            {+0.6476961374282837d, +2.4368339445199057E-8d,}, // 933
            {+0.6482070684432983d, -5.095229574221907E-8d,}, // 934
            {+0.6487176418304443d, -2.9485356677301627E-8d,}, // 935
            {+0.6492279767990112d, -3.0173901411577916E-8d,}, // 936
            {+0.649738073348999d, -5.275210583909726E-8d,}, // 937
            {+0.6502478122711182d, +2.2254737134350224E-8d,}, // 938
            {+0.6507574319839478d, -4.330693978322885E-8d,}, // 939
            {+0.6512666940689087d, -1.0753950588009912E-8d,}, // 940
            {+0.6517757177352905d, +9.686179886293545E-10d,}, // 941
            {+0.6522845029830933d, -7.875434494414498E-9d,}, // 942
            {+0.6527930498123169d, -3.702271091849158E-8d,}, // 943
            {+0.6533012390136719d, +3.2999073763758614E-8d,}, // 944
            {+0.6538093090057373d, -3.5966064858620067E-8d,}, // 945
            {+0.6543170213699341d, -5.23735298540578E-9d,}, // 946
            {+0.6548244953155518d, +6.237715351293023E-9d,}, // 947
            {+0.6553317308425903d, -1.279462699936282E-9d,}, // 948
            {+0.6558387279510498d, -2.7527887552743672E-8d,}, // 949
            {+0.6563453674316406d, +4.696233317356646E-8d,}, // 950
            {+0.6568518877029419d, -1.5967172745329108E-8d,}, // 951
            {+0.6573580503463745d, +2.2361985518423144E-8d,}, // 952
            {+0.657863974571228d, +4.2999935789083046E-8d,}, // 953
            {+0.6583696603775024d, +4.620570188811826E-8d,}, // 954
            {+0.6588751077651978d, +3.223791487908353E-8d,}, // 955
            {+0.659380316734314d, +1.3548138612715822E-9d,}, // 956
            {+0.6598852872848511d, -4.618575323863973E-8d,}, // 957
            {+0.6603899002075195d, +9.082960673843353E-9d,}, // 958
            {+0.6608942747116089d, +4.820873399634487E-8d,}, // 959
            {+0.6613985300064087d, -4.776104368314602E-8d,}, // 960
            {+0.6619024276733398d, -4.0151502150238136E-8d,}, // 961
            {+0.6624060869216919d, -4.791602708710648E-8d,}, // 962
            {+0.6629093885421753d, +4.8410188461165925E-8d,}, // 963
            {+0.6634125709533691d, +1.0663697110471944E-8d,}, // 964
            {+0.6639155149459839d, -4.1691464781797555E-8d,}, // 965
            {+0.66441810131073d, +1.080835500478704E-8d,}, // 966
            {+0.664920449256897d, +4.920784622407246E-8d,}, // 967
            {+0.6654226779937744d, -4.544868396511241E-8d,}, // 968
            {+0.6659245491027832d, -3.448944157854234E-8d,}, // 969
            {+0.6664261817932129d, -3.6870882345139385E-8d,}, // 970
            {+0.6669275760650635d, -5.234055273962444E-8d,}, // 971
            {+0.6674286127090454d, +3.856291077979099E-8d,}, // 972
            {+0.6679295301437378d, -2.327375671320742E-9d,}, // 973
            {+0.6684302091598511d, -5.555080534042001E-8d,}, // 974
            {+0.6689305305480957d, -1.6471487337453832E-9d,}, // 975
            {+0.6694306135177612d, +4.042486803683015E-8d,}, // 976
            {+0.6699305772781372d, -4.8293856891818295E-8d,}, // 977
            {+0.6704301834106445d, -2.9134931730784303E-8d,}, // 978
            {+0.6709295511245728d, -2.1058207594753368E-8d,}, // 979
            {+0.6714286804199219d, -2.3814619551682855E-8d,}, // 980
            {+0.6719275712966919d, -3.7155475428252136E-8d,}, // 981
            {+0.6724261045455933d, +5.8376834484391746E-8d,}, // 982
            {+0.6729245185852051d, +2.4611679969129262E-8d,}, // 983
            {+0.6734226942062378d, -1.899407107267079E-8d,}, // 984
            {+0.6739205121994019d, +4.7016079464436395E-8d,}, // 985
            {+0.6744182109832764d, -1.5529608026276525E-8d,}, // 986
            {+0.6749155521392822d, +3.203391672602453E-8d,}, // 987
            {+0.6754127740859985d, -4.8465821804075345E-8d,}, // 988
            {+0.6759096384048462d, -1.8364507801369988E-8d,}, // 989
            {+0.6764062643051147d, +3.3739397633046517E-9d,}, // 990
            {+0.6769026517868042d, +1.6994526063192333E-8d,}, // 991
            {+0.6773988008499146d, +2.2741891590028428E-8d,}, // 992
            {+0.6778947114944458d, +2.0860312877435047E-8d,}, // 993
            {+0.678390383720398d, +1.1593703222523284E-8d,}, // 994
            {+0.678885817527771d, -4.814386594291911E-9d,}, // 995
            {+0.6793810129165649d, -2.812076759125914E-8d,}, // 996
            {+0.6798759698867798d, -5.808261186903479E-8d,}, // 997
            {+0.680370569229126d, +2.4751837654582522E-8d,}, // 998
            {+0.6808650493621826d, -1.7793890245755405E-8d,}, // 999
            {+0.6813591718673706d, +5.294053246347931E-8d,}, // 1000
            {+0.681853175163269d, -1.2220826223585654E-9d,}, // 1001
            {+0.6823468208312988d, +5.8377876767612725E-8d,}, // 1002
            {+0.6828403472900391d, -6.437492120743254E-9d,}, // 1003
            {+0.6833335161209106d, +4.2990710043633113E-8d,}, // 1004
            {+0.6838265657424927d, -3.1516131027023284E-8d,}, // 1005
            {+0.684319257736206d, +8.70017386744679E-9d,}, // 1006
            {+0.6848117113113403d, +4.466959125843237E-8d,}, // 1007
            {+0.6853040456771851d, -4.25782656420497E-8d,}, // 1008
            {+0.6857960224151611d, -1.4386267593671393E-8d,}, // 1009
            {+0.6862877607345581d, +1.0274494061148778E-8d,}, // 1010
            {+0.686779260635376d, +3.164186629229597E-8d,}, // 1011
            {+0.6872705221176147d, +4.995334552140326E-8d,}, // 1012
            {+0.687761664390564d, -5.3763211240398744E-8d,}, // 1013
            {+0.6882524490356445d, -4.0852427502515625E-8d,}, // 1014
            {+0.688742995262146d, -3.0287143914420064E-8d,}, // 1015
            {+0.6892333030700684d, -2.183125937905008E-8d,}, // 1016
            {+0.6897233724594116d, -1.524901992178814E-8d,}, // 1017
            {+0.6902132034301758d, -1.0305018010328949E-8d,}, // 1018
            {+0.6907027959823608d, -6.764191876212205E-9d,}, // 1019
            {+0.6911921501159668d, -4.391824838015402E-9d,}, // 1020
            {+0.6916812658309937d, -2.9535446262017846E-9d,}, // 1021
            {+0.6921701431274414d, -2.2153227096187463E-9d,}, // 1022
            {+0.6926587820053101d, -1.943473623641502E-9d,}, // 1023
    };
}