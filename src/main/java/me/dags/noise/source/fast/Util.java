package me.dags.noise.source.fast;

/**
 * https://github.com/Auburns/FastNoise_Java
 */
public class Util {

    public final static int X_PRIME = 1619;
    public final static int Y_PRIME = 31337;
    public final static float CUBIC_2D_BOUNDING = 1 / (float) (1.5 * 1.5);

    public static final Float2[] GRAD_2D = {
            new Float2(-1, -1), new Float2(1, -1), new Float2(-1, 1), new Float2(1, 1),
            new Float2(0, -1), new Float2(-1, 0), new Float2(0, 1), new Float2(1, 0),
    };

    public static final Float2[] CELL_2D = {
            new Float2(-0.4313539279f, 0.1281943404f), new Float2(-0.1733316799f, 0.415278375f), new Float2(-0.2821957395f, -0.3505218461f), new Float2(-0.2806473808f, 0.3517627718f), new Float2(0.3125508975f, -0.3237467165f), new Float2(0.3383018443f, -0.2967353402f), new Float2(-0.4393982022f, -0.09710417025f), new Float2(-0.4460443703f, -0.05953502905f),
            new Float2(-0.302223039f, 0.3334085102f), new Float2(-0.212681052f, -0.3965687458f), new Float2(-0.2991156529f, 0.3361990872f), new Float2(0.2293323691f, 0.3871778202f), new Float2(0.4475439151f, -0.04695150755f), new Float2(0.1777518f, 0.41340573f), new Float2(0.1688522499f, -0.4171197882f), new Float2(-0.0976597166f, 0.4392750616f),
            new Float2(0.08450188373f, 0.4419948321f), new Float2(-0.4098760448f, -0.1857461384f), new Float2(0.3476585782f, -0.2857157906f), new Float2(-0.3350670039f, -0.30038326f), new Float2(0.2298190031f, -0.3868891648f), new Float2(-0.01069924099f, 0.449872789f), new Float2(-0.4460141246f, -0.05976119672f), new Float2(0.3650293864f, 0.2631606867f),
            new Float2(-0.349479423f, 0.2834856838f), new Float2(-0.4122720642f, 0.1803655873f), new Float2(-0.267327811f, 0.3619887311f), new Float2(0.322124041f, -0.3142230135f), new Float2(0.2880445931f, -0.3457315612f), new Float2(0.3892170926f, -0.2258540565f), new Float2(0.4492085018f, -0.02667811596f), new Float2(-0.4497724772f, 0.01430799601f),
            new Float2(0.1278175387f, -0.4314657307f), new Float2(-0.03572100503f, 0.4485799926f), new Float2(-0.4297407068f, -0.1335025276f), new Float2(-0.3217817723f, 0.3145735065f), new Float2(-0.3057158873f, 0.3302087162f), new Float2(-0.414503978f, 0.1751754899f), new Float2(-0.3738139881f, 0.2505256519f), new Float2(0.2236891408f, -0.3904653228f),
            new Float2(0.002967775577f, -0.4499902136f), new Float2(0.1747128327f, -0.4146991995f), new Float2(-0.4423772489f, -0.08247647938f), new Float2(-0.2763960987f, -0.355112935f), new Float2(-0.4019385906f, -0.2023496216f), new Float2(0.3871414161f, -0.2293938184f), new Float2(-0.430008727f, 0.1326367019f), new Float2(-0.03037574274f, -0.4489736231f),
            new Float2(-0.3486181573f, 0.2845441624f), new Float2(0.04553517144f, -0.4476902368f), new Float2(-0.0375802926f, 0.4484280562f), new Float2(0.3266408905f, 0.3095250049f), new Float2(0.06540017593f, -0.4452222108f), new Float2(0.03409025829f, 0.448706869f), new Float2(-0.4449193635f, 0.06742966669f), new Float2(-0.4255936157f, -0.1461850686f),
            new Float2(0.449917292f, 0.008627302568f), new Float2(0.05242606404f, 0.4469356864f), new Float2(-0.4495305179f, -0.02055026661f), new Float2(-0.1204775703f, 0.4335725488f), new Float2(-0.341986385f, -0.2924813028f), new Float2(0.3865320182f, 0.2304191809f), new Float2(0.04506097811f, -0.447738214f), new Float2(-0.06283465979f, 0.4455915232f),
            new Float2(0.3932600341f, -0.2187385324f), new Float2(0.4472261803f, -0.04988730975f), new Float2(0.3753571011f, -0.2482076684f), new Float2(-0.273662295f, 0.357223947f), new Float2(0.1700461538f, 0.4166344988f), new Float2(0.4102692229f, 0.1848760794f), new Float2(0.323227187f, -0.3130881435f), new Float2(-0.2882310238f, -0.3455761521f),
            new Float2(0.2050972664f, 0.4005435199f), new Float2(0.4414085979f, -0.08751256895f), new Float2(-0.1684700334f, 0.4172743077f), new Float2(-0.003978032396f, 0.4499824166f), new Float2(-0.2055133639f, 0.4003301853f), new Float2(-0.006095674897f, -0.4499587123f), new Float2(-0.1196228124f, -0.4338091548f), new Float2(0.3901528491f, -0.2242337048f),
            new Float2(0.01723531752f, 0.4496698165f), new Float2(-0.3015070339f, 0.3340561458f), new Float2(-0.01514262423f, -0.4497451511f), new Float2(-0.4142574071f, -0.1757577897f), new Float2(-0.1916377265f, -0.4071547394f), new Float2(0.3749248747f, 0.2488600778f), new Float2(-0.2237774255f, 0.3904147331f), new Float2(-0.4166343106f, -0.1700466149f),
            new Float2(0.3619171625f, 0.267424695f), new Float2(0.1891126846f, -0.4083336779f), new Float2(-0.3127425077f, 0.323561623f), new Float2(-0.3281807787f, 0.307891826f), new Float2(-0.2294806661f, 0.3870899429f), new Float2(-0.3445266136f, 0.2894847362f), new Float2(-0.4167095422f, -0.1698621719f), new Float2(-0.257890321f, -0.3687717212f),
            new Float2(-0.3612037825f, 0.2683874578f), new Float2(0.2267996491f, 0.3886668486f), new Float2(0.207157062f, 0.3994821043f), new Float2(0.08355176718f, -0.4421754202f), new Float2(-0.4312233307f, 0.1286329626f), new Float2(0.3257055497f, 0.3105090899f), new Float2(0.177701095f, -0.4134275279f), new Float2(-0.445182522f, 0.06566979625f),
            new Float2(0.3955143435f, 0.2146355146f), new Float2(-0.4264613988f, 0.1436338239f), new Float2(-0.3793799665f, -0.2420141339f), new Float2(0.04617599081f, -0.4476245948f), new Float2(-0.371405428f, -0.2540826796f), new Float2(0.2563570295f, -0.3698392535f), new Float2(0.03476646309f, 0.4486549822f), new Float2(-0.3065454405f, 0.3294387544f),
            new Float2(-0.2256979823f, 0.3893076172f), new Float2(0.4116448463f, -0.1817925206f), new Float2(-0.2907745828f, -0.3434387019f), new Float2(0.2842278468f, -0.348876097f), new Float2(0.3114589359f, -0.3247973695f), new Float2(0.4464155859f, -0.0566844308f), new Float2(-0.3037334033f, -0.3320331606f), new Float2(0.4079607166f, 0.1899159123f),
            new Float2(-0.3486948919f, -0.2844501228f), new Float2(0.3264821436f, 0.3096924441f), new Float2(0.3211142406f, 0.3152548881f), new Float2(0.01183382662f, 0.4498443737f), new Float2(0.4333844092f, 0.1211526057f), new Float2(0.3118668416f, 0.324405723f), new Float2(-0.272753471f, 0.3579183483f), new Float2(-0.422228622f, -0.1556373694f),
            new Float2(-0.1009700099f, -0.4385260051f), new Float2(-0.2741171231f, -0.3568750521f), new Float2(-0.1465125133f, 0.4254810025f), new Float2(0.2302279044f, -0.3866459777f), new Float2(-0.3699435608f, 0.2562064828f), new Float2(0.105700352f, -0.4374099171f), new Float2(-0.2646713633f, 0.3639355292f), new Float2(0.3521828122f, 0.2801200935f),
            new Float2(-0.1864187807f, -0.4095705534f), new Float2(0.1994492955f, -0.4033856449f), new Float2(0.3937065066f, 0.2179339044f), new Float2(-0.3226158377f, 0.3137180602f), new Float2(0.3796235338f, 0.2416318948f), new Float2(0.1482921929f, 0.4248640083f), new Float2(-0.407400394f, 0.1911149365f), new Float2(0.4212853031f, 0.1581729856f),
            new Float2(-0.2621297173f, 0.3657704353f), new Float2(-0.2536986953f, -0.3716678248f), new Float2(-0.2100236383f, 0.3979825013f), new Float2(0.3624152444f, 0.2667493029f), new Float2(-0.3645038479f, -0.2638881295f), new Float2(0.2318486784f, 0.3856762766f), new Float2(-0.3260457004f, 0.3101519002f), new Float2(-0.2130045332f, -0.3963950918f),
            new Float2(0.3814998766f, -0.2386584257f), new Float2(-0.342977305f, 0.2913186713f), new Float2(-0.4355865605f, 0.1129794154f), new Float2(-0.2104679605f, 0.3977477059f), new Float2(0.3348364681f, -0.3006402163f), new Float2(0.3430468811f, 0.2912367377f), new Float2(-0.2291836801f, -0.3872658529f), new Float2(0.2547707298f, -0.3709337882f),
            new Float2(0.4236174945f, -0.151816397f), new Float2(-0.15387742f, 0.4228731957f), new Float2(-0.4407449312f, 0.09079595574f), new Float2(-0.06805276192f, -0.444824484f), new Float2(0.4453517192f, -0.06451237284f), new Float2(0.2562464609f, -0.3699158705f), new Float2(0.3278198355f, -0.3082761026f), new Float2(-0.4122774207f, -0.1803533432f),
            new Float2(0.3354090914f, -0.3000012356f), new Float2(0.446632869f, -0.05494615882f), new Float2(-0.1608953296f, 0.4202531296f), new Float2(-0.09463954939f, 0.4399356268f), new Float2(-0.02637688324f, -0.4492262904f), new Float2(0.447102804f, -0.05098119915f), new Float2(-0.4365670908f, 0.1091291678f), new Float2(-0.3959858651f, 0.2137643437f),
            new Float2(-0.4240048207f, -0.1507312575f), new Float2(-0.3882794568f, 0.2274622243f), new Float2(-0.4283652566f, -0.1378521198f), new Float2(0.3303888091f, 0.305521251f), new Float2(0.3321434919f, -0.3036127481f), new Float2(-0.413021046f, -0.1786438231f), new Float2(0.08403060337f, -0.4420846725f), new Float2(-0.3822882919f, 0.2373934748f),
            new Float2(-0.3712395594f, -0.2543249683f), new Float2(0.4472363971f, -0.04979563372f), new Float2(-0.4466591209f, 0.05473234629f), new Float2(0.0486272539f, -0.4473649407f), new Float2(-0.4203101295f, -0.1607463688f), new Float2(0.2205360833f, 0.39225481f), new Float2(-0.3624900666f, 0.2666476169f), new Float2(-0.4036086833f, -0.1989975647f),
            new Float2(0.2152727807f, 0.3951678503f), new Float2(-0.4359392962f, -0.1116106179f), new Float2(0.4178354266f, 0.1670735057f), new Float2(0.2007630161f, 0.4027334247f), new Float2(-0.07278067175f, -0.4440754146f), new Float2(0.3644748615f, -0.2639281632f), new Float2(-0.4317451775f, 0.126870413f), new Float2(-0.297436456f, 0.3376855855f),
            new Float2(-0.2998672222f, 0.3355289094f), new Float2(-0.2673674124f, 0.3619594822f), new Float2(0.2808423357f, 0.3516071423f), new Float2(0.3498946567f, 0.2829730186f), new Float2(-0.2229685561f, 0.390877248f), new Float2(0.3305823267f, 0.3053118493f), new Float2(-0.2436681211f, -0.3783197679f), new Float2(-0.03402776529f, 0.4487116125f),
            new Float2(-0.319358823f, 0.3170330301f), new Float2(0.4454633477f, -0.06373700535f), new Float2(0.4483504221f, 0.03849544189f), new Float2(-0.4427358436f, -0.08052932871f), new Float2(0.05452298565f, 0.4466847255f), new Float2(-0.2812560807f, 0.3512762688f), new Float2(0.1266696921f, 0.4318041097f), new Float2(-0.3735981243f, 0.2508474468f),
            new Float2(0.2959708351f, -0.3389708908f), new Float2(-0.3714377181f, 0.254035473f), new Float2(-0.404467102f, -0.1972469604f), new Float2(0.1636165687f, -0.419201167f), new Float2(0.3289185495f, -0.3071035458f), new Float2(-0.2494824991f, -0.3745109914f), new Float2(0.03283133272f, 0.4488007393f), new Float2(-0.166306057f, -0.4181414777f),
            new Float2(-0.106833179f, 0.4371346153f), new Float2(0.06440260376f, -0.4453676062f), new Float2(-0.4483230967f, 0.03881238203f), new Float2(-0.421377757f, -0.1579265206f), new Float2(0.05097920662f, -0.4471030312f), new Float2(0.2050584153f, -0.4005634111f), new Float2(0.4178098529f, -0.167137449f), new Float2(-0.3565189504f, -0.2745801121f),
            new Float2(0.4478398129f, 0.04403977727f), new Float2(-0.3399999602f, -0.2947881053f), new Float2(0.3767121994f, 0.2461461331f), new Float2(-0.3138934434f, 0.3224451987f), new Float2(-0.1462001792f, -0.4255884251f), new Float2(0.3970290489f, -0.2118205239f), new Float2(0.4459149305f, -0.06049689889f), new Float2(-0.4104889426f, -0.1843877112f),
            new Float2(0.1475103971f, -0.4251360756f), new Float2(0.09258030352f, 0.4403735771f), new Float2(-0.1589664637f, -0.4209865359f), new Float2(0.2482445008f, 0.3753327428f), new Float2(0.4383624232f, -0.1016778537f), new Float2(0.06242802956f, 0.4456486745f), new Float2(0.2846591015f, -0.3485243118f), new Float2(-0.344202744f, -0.2898697484f),
            new Float2(0.1198188883f, -0.4337550392f), new Float2(-0.243590703f, 0.3783696201f), new Float2(0.2958191174f, -0.3391033025f), new Float2(-0.1164007991f, 0.4346847754f), new Float2(0.1274037151f, -0.4315881062f), new Float2(0.368047306f, 0.2589231171f), new Float2(0.2451436949f, 0.3773652989f), new Float2(-0.4314509715f, 0.12786735f),
    };

    public static int FastFloor(float f) {
        return (f >= 0 ? (int) f : (int) f - 1);
    }

    public static int FastRound(float f) {
        return (f >= 0) ? (int) (f + (float) 0.5) : (int) (f - (float) 0.5);
    }

    public static float Lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static float InterpHermiteFunc(float t) {
        return t * t * (3 - 2 * t);
    }

    public static float InterpQuinticFunc(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static float CubicLerp(float a, float b, float c, float d, float t) {
        float p = (d - c) - (a - b);
        return t * t * t * p + t * t * ((a - b) - p) + t * (c - a) + b;
    }

    public static int Hash2D(int seed, int x, int y) {
        int hash = seed;
        hash ^= X_PRIME * x;
        hash ^= Y_PRIME * y;

        hash = hash * hash * hash * 60493;
        hash = (hash >> 13) ^ hash;

        return hash;
    }

    public static float ValCoord2D(int seed, int x, int y) {
        int n = seed;
        n ^= X_PRIME * x;
        n ^= Y_PRIME * y;

        return (n * n * n * 60493) / (float) 2147483648.0;
    }

    public static float GradCoord2D(int seed, int x, int y, float xd, float yd) {
        int hash = seed;
        hash ^= X_PRIME * x;
        hash ^= Y_PRIME * y;

        hash = hash * hash * hash * 60493;
        hash = (hash >> 13) ^ hash;

        Float2 g = GRAD_2D[hash & 7];

        return xd * g.x + yd * g.y;
    }

    public static class Float2 {

        public final float x, y;

        public Float2(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
