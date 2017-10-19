package utils.functionality

/**
 * Shayan Rais (http://shanraisshan.com) created on 8/1/2016
 */
enum class Emoji(val symbol: String) {
    //Row#: 1
    GREEN_APPLE("🍏"), //https://www.emojibase.com/emoji/1f34f/greenapple
    RED_APPLE("🍎"), //https://www.emojibase.com/emoji/1f34e/redapple
    PEAR("🍐"), //https://www.emojibase.com/emoji/1f350/pear
    TANGERINE("🍊"), //https://www.emojibase.com/emoji/1f34a/tangerine
    LEMON("🍋"), //https://www.emojibase.com/emoji/1f34b/lemon
    BANANA("🍌"), //https://www.emojibase.com/emoji/1f34c/banana
    WATERMELON("🍉"), //https://www.emojibase.com/emoji/1f349/watermelon
    GRAPES("🍇"), //https://www.emojibase.com/emoji/1f347/grapes

    //Row#: 2
    STRAWBERRY("🍓"), //https://www.emojibase.com/emoji/1f353/strawberry
    MELON("🍈"), //https://www.emojibase.com/emoji/1f348/melon
    CHERRY("🍒"), //https://www.emojibase.com/emoji/1f352/cherries
    PEACH("🍑"), //https://www.emojibase.com/emoji/1f351/peach
    PINEAPPLE("🍍"), //https://www.emojibase.com/emoji/1f34d/pineapple
    TOMATO("🍅"), //https://www.emojibase.com/emoji/1f345/tomato
    EGG_PLANT("🍆"), //https://www.emojibase.com/emoji/1f346/eggplant
    HOT_PEPPER("🌶"), //https://www.emojibase.com/emoji/1f336/hotpepper

    //Row#: 3
    EAR_OF_MAIZE("🌽"), //https://www.emojibase.com/emoji/1f33d/earofmaize
    ROASTED_SWEET_POTATO("🍠"), //https://www.emojibase.com/emoji/1f360/roastedsweetpotato
    HONEY_POT("🍯"), //https://www.emojibase.com/emoji/1f36f/honeypot
    BREAD("🍞"), //https://www.emojibase.com/emoji/1f35e/bread
    CHEESE("🧀"), //http://emojipedia.org/cheese-wedge/
    POULTRY_LEG("🍗"), //https://www.emojibase.com/emoji/1f357/poultryleg
    MEAT_ON_BONE("🍖"), //https://www.emojibase.com/emoji/1f356/meatonbone
    FRIED_SHRIMP("🍤"), //https://www.emojibase.com/emoji/1f364/friedshrimp

    //Row#: 4
    COOKING("🍳"), //https://www.emojibase.com/emoji/1f373/cooking
    HAMBURGER("🍔"), //https://www.emojibase.com/emoji/1f354/hamburger
    FRENCH_FRIES("🍟"), //https://www.emojibase.com/emoji/1f35f/frenchfries
    HOT_DOG("🌭"), //http://emojipedia.org/hot-dog/
    SLICE_OF_PIZZA("🍕"), //https://www.emojibase.com/emoji/1f355/sliceofpizza
    SPAGHETTI("🍝"), //https://www.emojibase.com/emoji/1f35d/spaghetti
    TACO("🌮"), //http://emojipedia.org/taco/
    BURRITO("🌯"), //http://emojipedia.org/burrito/

    //Row#: 5
    STEAMING_BOWL("🍜"), //https://www.emojibase.com/emoji/1f35c/steamingbowl
    POT_OF_FOOD("🍲"), //https://www.emojibase.com/emoji/1f372/potoffood
    FISH_CAKE("🍥"), //https://www.emojibase.com/emoji/1f365/fishcakewithswirldesign
    SUSHI("🍣"), //https://www.emojibase.com/emoji/1f363/sushi
    BENTO_BOX("🍱"), //https://www.emojibase.com/emoji/1f371/bentobox
    CURRY_AND_RICE("🍛"), //https://www.emojibase.com/emoji/1f35b/curryandrice
    RICE_BALL("🍙"), //https://www.emojibase.com/emoji/1f359/riceball
    COOKED_RICE("🍚"), //https://www.emojibase.com/emoji/1f35a/cookedrice

    //Row#: 6
    RICE_CRACKER("🍘"), //https://www.emojibase.com/emoji/1f358/ricecracker
    ODEN("🍢"), //https://www.emojibase.com/emoji/1f362/oden
    DANGO("🍡"), //https://www.emojibase.com/emoji/1f361/dango
    SHAVED_ICE("🍧"), //https://www.emojibase.com/emoji/1f367/shavedice
    ICE_CREAM("🍨"), //https://www.emojibase.com/emoji/1f368/icecream
    SOFT_ICE_CREAM("🍦"), //https://www.emojibase.com/emoji/1f366/softicecream
    SHORT_CAKE("🍰"), //https://www.emojibase.com/emoji/1f370/shortcake
    BIRTHDAY_CAKE("🎂"), //https://www.emojibase.com/emoji/1f382/birthdaycake

    //Row#: 7
    CUSTARD("🍮"), //https://www.emojibase.com/emoji/1f36e/custard
    CANDY("🍬"), //https://www.emojibase.com/emoji/1f36c/candy
    LOLLIPOP("🍭"), //https://www.emojibase.com/emoji/1f36d/lollipop
    CHOCOLATE_BAR("🍫"), //https://www.emojibase.com/emoji/1f36b/chocolatebar
    POPCORN("🍿"), //http://emojipedia.org/popcorn/
    DOUGHNUT("🍩"), //https://www.emojibase.com/emoji/1f369/doughnut
    COOKIE("🍪"), //https://www.emojibase.com/emoji/1f36a/cookie
    BEAR_MUG("🍺"), //https://www.emojibase.com/emoji/1f37a/beermug

    //Row#: 8
    CLINKING_BEER_MUGS("🍻"), //https://www.emojibase.com/emoji/1f37b/clinkingbeermugs
    WINE_GLASS("🍷"), //https://www.emojibase.com/emoji/1f377/wineglass
    COCKTAIL_GLASS("🍸"), //https://www.emojibase.com/emoji/1f378/cocktailglass
    TROPICAL_DRINK("🍹"), //https://www.emojibase.com/emoji/1f379/tropicaldrink
    BOTTLE_WITH_POPPING_CORK("🍾"), //http://emojipedia.org/bottle-with-popping-cork/
    SAKE_BOTTLE_AND_CUP("🍶"), //https://www.emojibase.com/emoji/1f376/sakebottleandcup
    TEA_CUP_WITHOUT_HANDLE("🍵"), //https://www.emojibase.com/emoji/1f375/teacupwithouthandle
    HOT_BEVERAGE("☕"), //https://www.emojibase.com/emoji/2615/hotbeverage

    //Row#: 9
    BABY_BOTTLE("🍼"), //https://www.emojibase.com/emoji/1f37c/babybottle
    FORK_AND_KNIFE("🍴"), //https://www.emojibase.com/emoji/1f374/forkandknife
    FORK_AND_KNIFE_WITH_PLATE("🍽"), //https://www.emojibase.com/emoji/1f37d/forkandknifewithplate

    //Row#: 1
    HEAVY_BLACK_HEART("❤"), //http://www.emojibase.com/emoji/2764/heavyblackheart
    YELLOW_HEART("💛"), //http://www.emojibase.com/emoji/1f49b/yellowheart
    GREEN_HEART("💚"), //http://www.emojibase.com/emoji/1f49a/greenheart
    BLUE_HEART("💙"), //http://www.emojibase.com/emoji/1f499/blueheart
    PURPLE_HEART("💜"), //http://www.emojibase.com/emoji/1f49c/purpleheart
    BROKEN_HEART("💔"), //http://www.emojibase.com/emoji/1f494/brokenheart
    HEAVY_HEART_EXCLAMATION_MARK_ORNAMENT("❣"), //http://www.emojibase.com/emoji/2763/heavyheartexclamationmarkornament
    TWO_HEARTS("💕"), //http://www.emojibase.com/emoji/1f495/twohearts

    //Row#: 2
    REVOLVING_HEARTS("💞"), //http://www.emojibase.com/emoji/1f49e/revolvinghearts
    BEATING_HEART("💓"), //http://www.emojibase.com/emoji/1f493/beatingheart
    GROWING_HEART("💗"), //http://www.emojibase.com/emoji/1f497/growingheart
    SPARKLING_HEART("💖"), //http://www.emojibase.com/emoji/1f496/sparklingheart
    HEART_WITH_ARROW("💘"), //http://www.emojibase.com/emoji/1f498/heartwitharrow
    HEART_WITH_RIBBON("💝"), //http://www.emojibase.com/emoji/1f49d/heartwithribbon
    HEART_DECORATION("💟"), //http://www.emojibase.com/emoji/1f49f/heartdecoration
    PEACE_SYMBOL("☮"), //http://www.emojibase.com/emoji/262e/peacesymbol

    //Row#: 3
    LATIN_CROSS("✝"), //http://www.emojipedia.org/latin-cross/
    STAR_AND_CRESCENT("☪"), //http://www.emojipedia.org/star-and-crescent/
    OM_SYMBOL("🕉"), //http://www.emojipedia.org/om-symbol/
    WHEEL_OF_DHARMA("☸"), //http://www.emojipedia.org/wheel-of-dharma/
    STAR_OF_DAVID("✡"), //http://www.emojipedia.org/star-of-david/
    SIX_POINTED_STAR_WITH_MIDDLE_DOT("🔯"), //http://www.emojibase.com/emoji/1f52f/sixpointedstarwithmiddledot
    MENORAH_WITH_NINE_BRANCHES("🕎"), //http://www.emojipedia.org/menorah-with-nine-branches/
    YIN_YANG("☯"), //http://www.emojibase.com/emoji/262f/yinyang

    //Row#: 4
    ORTHODOX_CROSS("☦"), //http://www.emojipedia.org/orthodox-cross/
    PLACE_OF_WORSHIP("🛐"), //http://www.emojipedia.org/place-of-worship/
    OPHIUCHUS("⛎"), //http://www.emojibase.com/emoji/26ce/ophiuchus
    ARIES("♈"), //http://www.emojibase.com/emoji/2648/aries
    TAURUS("♉"), //http://www.emojibase.com/emoji/2649/taurus
    GEMINI("♊"), //https://www.emojibase.com/emoji/264a/gemini
    CANCER("♋"), //http://www.emojibase.com/emoji/264b/cancer
    LEO("♌"), //http://www.emojibase.com/emoji/264c/leo

    //Row#: 5
    VIRGO("♍"), //http://www.emojibase.com/emoji/264d/virgo
    LIBRA("♎"), //http://www.emojibase.com/emoji/264e/libra
    SCORPIUS("♏"), //http://www.emojibase.com/emoji/264f/scorpius
    SAGITTARIUS("♐"), //http://www.emojibase.com/emoji/2650/sagittarius
    CAPRICORN("♑"), //http://www.emojibase.com/emoji/2651/capricorn
    AQUARIUS("♒"), //http://www.emojibase.com/emoji/2652/aquarius
    PISCES("♓"), //http://www.emojibase.com/emoji/2653/pisces
    SQUARED_ID("🆔"), //http://www.emojibase.com/emoji/1f194/squaredid

    //Row#: 6
    ATOM_SYMBOL("⚛"), //http://www.emojibase.com/emoji/269b/atomsymbol
    SQUARED_CJK_UNIFIED_IDEOGRAPH_7A7A("🈳"), //http://www.emojipedia.org/squared-cjk-unified-ideograph-7a7a/
    SQUARED_CJK_UNIFIED_IDEOGRAPH_5272("🈹"), //http://www.emojibase.com/emoji/1f239/squaredcjkunifiedideograph5272
    RADIOACTIVE_SIGN("☢"), //http://www.emojibase.com/emoji/2622/radioactivesign
    BIOHAZARD_SIGN("☣"), //http://www.emojibase.com/emoji/2623/biohazardsign
    MOBILE_PHONE_OFF("📴"), //http://www.emojibase.com/emoji/1f4f4/mobilephoneoff
    VIBRATION_MODE("📳"), //http://www.emojibase.com/emoji/1f4f3/vibrationmode
    SQUARED_CJK_UNIFIED_IDEOGRAPH_6709("🈶"), //http://www.emojibase.com/emoji/1f236/squaredcjkunifiedideograph6709

    //Row#: 7
    SQUARED_CJK_UNIFIED_IDEOGRAPH_7121("🈚"), //http://www.emojipedia.org/squared-cjk-unified-ideograph-7121/
    SQUARED_CJK_UNIFIED_IDEOGRAPH_7533("🈸"), //http://www.emojibase.com/emoji/1f238/squaredcjkunifiedideograph7533
    SQUARED_CJK_UNIFIED_IDEOGRAPH_55B6("🈺"), //http://www.emojibase.com/emoji/1f23a/squaredcjkunifiedideograph55b6
    SQUARED_CJK_UNIFIED_IDEOGRAPH_6708("🈷"), //http://www.emojibase.com/emoji/1f237/squaredcjkunifiedideograph6708
    EIGHT_POINTED_BLACK_STAR("✴"), //http://www.emojibase.com/emoji/2734/eightpointedblackstar
    SQUARED_VS("🆚"), //http://www.emojibase.com/emoji/1f19a/squaredvs
    CIRCLED_IDEOGRAPH_ACCEPT("🉑"), //http://www.emojibase.com/emoji/1f251/circledideographaccept
    WHITE_FLOWER("💮"), //http://www.emojibase.com/emoji/1f4ae/whiteflower

    //Row#: 8
    CIRCLED_IDEOGRAPH_ADVANTAGE("🉐"), //http://www.emojibase.com/emoji/1f250/circledideographadvantage
    CIRCLED_IDEOGRAPH_SECRET("㊙"), //http://www.emojibase.com/emoji/3299/circledideographsecret
    CIRCLED_IDEOGRAPH_CONGRATULATION("㊗"), //http://www.emojibase.com/emoji/3297/circledideographcongratulation
    SQUARED_CJK_UNIFIED_IDEOGRAPH_5408("🈴"), //http://www.emojibase.com/emoji/1f234/squaredcjkunifiedideograph5408
    SQUARED_CJK_UNIFIED_IDEOGRAPH_6E80("🈵"), //http://www.emojibase.com/emoji/1f235/squaredcjkunifiedideograph6e80
    SQUARED_CJK_UNIFIED_IDEOGRAPH_7981("🈲"), //http://www.emojibase.com/emoji/1f232/squaredcjkunifiedideograph7981
    NEGATIVE_SQUARED_LATIN_CAPITAL_LETTER_A("🅰"), //http://www.emojibase.com/emoji/1f170/negativesquaredlatincapitallettera
    NEGATIVE_SQUARED_LATIN_CAPITAL_LETTER_B("🅱"), //http://www.emojibase.com/emoji/1f171/negativesquaredlatincapitalletterb

    //Row#: 9
    NEGATIVE_SQUARED_AB("🆎"), //http://www.emojibase.com/emoji/1f18e/negativesquaredab
    SQUARED_CL("🆑"), //http://www.emojibase.com/emoji/1f191/squaredcl
    NEGATIVE_SQUARED_LATIN_CAPITAL_LETTER_O("🅾"), //http://www.emojibase.com/emoji/1f17e/negativesquaredlatincapitallettero
    SQUARED_SOS("🆘"), //http://www.emojibase.com/emoji/1f198/squaredsos
    NO_ENTRY("⛔"), //http://www.emojibase.com/emoji/26d4/noentry
    NAME_BADGE("📛"), //http://www.emojibase.com/emoji/1f4db/namebadge
    NO_ENTRY_SIGN("🚫"), //http://www.emojibase.com/emoji/1f6ab/noentrysign
    CROSS_MARK("❌"), //http://www.emojibase.com/emoji/274c/crossmark

    //Row#: 10
    HEAVY_LARGE_CIRCLE("⭕"), //http://www.emojibase.com/emoji/2b55/heavylargecircle
    ANGER_SYMBOL("💢"), //http://www.emojibase.com/emoji/1f4a2/angersymbol
    HOT_SPRINGS("♨"), //http://www.emojibase.com/emoji/2668/hotsprings
    NO_PEDESTRIANS("🚷"), //http://www.emojibase.com/emoji/1f6b7/nopedestrians
    DO_NOT_LITTER_SYMBOL("🚯"), //http://www.emojibase.com/emoji/1f6af/donotlittersymbol
    NO_BI_CYCLES("🚳"), //http://www.emojibase.com/emoji/1f6b3/nobicycles
    NON_POTABLE_WATER_SYMBOL("🚱"), //http://www.emojibase.com/emoji/1f6b1/nonpotablewatersymbol
    NO_ONE_UNDER_EIGHTEEN_SYMBOL("🔞"), //http://www.emojibase.com/emoji/1f51e/nooneundereighteensymbol

    //Row#: 11
    NO_MOBILE_PHONES("📵"), //http://www.emojibase.com/emoji/1f4f5/nomobilephones
    HEAVY_EXCLAMATION_MARK_SYMBOL("❗"), //http://www.emojibase.com/emoji/2757/heavyexclamationmarksymbol
    WHITE_EXCLAMATION_MARK_ORNAMENT("❕"), //http://www.emojibase.com/emoji/2755/whiteexclamationmarkornament
    BLACK_QUESTION_MARK_ORNAMENT("❓"), //http://www.emojibase.com/emoji/2753/blackquestionmarkornament
    WHITE_QUESTION_MARK_ORNAMENT("❔"), //http://www.emojibase.com/emoji/2754/whitequestionmarkornament
    DOUBLE_EXCLAMATION_MARK("‼"), //http://www.emojibase.com/emoji/203c/doubleexclamationmark
    EXCLAMATION_QUESTION_MARK("⁉"), //http://www.emojibase.com/emoji/2049/exclamationquestionmark
    HUNDRED_POINTS_SYMBOL("💯"), //http://www.emojibase.com/emoji/1f4af/hundredpointssymbol

    //Row#: 12
    LOW_BRIGHTNESS_SYMBOL("🔅"), //http://www.emojibase.com/emoji/1f505/lowbrightnesssymbol
    HIGH_BRIGHTNESS_SYMBOL("🔆"), //http://www.emojibase.com/emoji/1f506/highbrightnesssymbol
    TRIDENT_EMBLEM("🔱"), //http://www.emojibase.com/emoji/1f531/tridentemblem
    FLEUR_DE_LIS("⚜"), //http://www.emojibase.com/emoji/269c/fleurdelis
    PART_ALTERNATION_MARK("〽"), //http://www.emojibase.com/emoji/303d/partalternationmark
    WARNING_SIGN("⚠"), //http://www.emojibase.com/emoji/26a0/warningsign
    CHILDREN_CROSSING("🚸"), //http://www.emojibase.com/emoji/1f6b8/childrencrossing
    JAPANESE_SYMBOL_FOR_BEGINNER("🔰"), //http://www.emojibase.com/emoji/1f530/japanesesymbolforbeginner

    //Row#: 13
    BLACK_UNIVERSAL_RECYCLING_SYMBOL("♻"), //http://www.emojibase.com/emoji/267b/blackuniversalrecyclingsymbol
    SQUARED_CJK_UNIFIED_IDEOGRAPH_6307("🈯"), //http://www.emojibase.com/emoji/1f22f/squaredcjkunifiedideograph6307
    CHART_WITH_UPWARDS_TREND_AND_YEN_SIGN("💹"), //http://www.emojibase.com/emoji/1f4b9/chartwithupwardstrendandyensign
    SPARKLE("❇"), //http://www.emojibase.com/emoji/2747/sparkle
    EIGHT_SPOKED_ASTERISK("✳"), //http://www.emojibase.com/emoji/2733/eightspokedasterisk
    NEGATIVE_SQUARED_CROSSMARK("❎"), //http://www.emojibase.com/emoji/274e/negativesquaredcrossmark
    WHITE_HEAVY_CHECKMARK("✅"), //http://www.emojibase.com/emoji/2705/whiteheavycheckmark
    DIAMOND_SHAPE_WITH_A_DOT_INSIDE("💠"), //http://www.emojibase.com/emoji/1f4a0/diamondshapewithadotinside

    //Row#: 14
    CYCLONE("🌀"), //http://www.emojibase.com/emoji/1f300/cyclone
    DOUBLE_CURLY_LOOP("➿"), //http://www.emojibase.com/emoji/27bf/doublecurlyloop
    GLOBE_WITH_MERIDIANS("🌐"), //http://www.emojibase.com/emoji/1f310/globewithmeridians
    CIRCLED_LATIN_CAPITAL_LETTER_M("Ⓜ"), //http://www.emojibase.com/emoji/24c2/circledlatincapitalletterm
    AUTOMATED_TELLER_MACHINE("🏧"), //http://www.emojibase.com/emoji/1f3e7/automatedtellermachine
    SQUARED_KATAKANASA("🈂"), //http://www.emojibase.com/emoji/1f202/squaredkatakanasa
    PASSPORT_CONTROL("🛂"), //http://www.emojibase.com/emoji/1f6c2/passportcontrol
    CUSTOMS("🛃"), //http://www.emojibase.com/emoji/1f6c3/customs

    //Row#: 15
    BAGGAGE_CLAIM("🛄"), //http://www.emojibase.com/emoji/1f6c4/baggageclaim
    LEFT_LUGGAGE("🛅"), //http://www.emojibase.com/emoji/1f6c5/leftluggage
    WHEEL_CHAIR_SYMBOL("♿"), //http://www.emojibase.com/emoji/267f/wheelchairsymbol
    NO_SMOKING_SYMBOL("🚭"), //http://www.emojibase.com/emoji/1f6ad/nosmokingsymbol
    WATER_CLOSET("🚾"), //http://www.emojibase.com/emoji/1f6be/watercloset
    NEGATIVE_SQUARED_LETTER_P("🅿"), //http://www.emojibase.com/emoji/1f17f/negativesquaredlatincapitalletterp
    POTABLE_WATER_SYMBOL("🚰"), //http://www.emojibase.com/emoji/1f6b0/potablewatersymbol
    MENS_SYMBOL("🚹"), //http://www.emojibase.com/emoji/1f6b9/menssymbol

    //Row#: 16
    WOMENS_SYMBOL("🚺"), //http://www.emojibase.com/emoji/1f6ba/womenssymbol
    BABY_SYMBOL("🚼"), //http://www.emojibase.com/emoji/1f6bc/babysymbol
    RESTROOM("🚻"), //http://www.emojibase.com/emoji/1f6bb/restroom
    PUT_LITTER_IN_ITS_PLACE("🚮"), //http://www.emojibase.com/emoji/1f6ae/putlitterinitsplacesymbol
    CINEMA("🎦"), //http://www.emojibase.com/emoji/1f3a6/cinema
    ANTENNA_WITH_BARS("📶"), //http://www.emojibase.com/emoji/1f4f6/antennawithbars
    SQUARED_KATAKANA_KOKO("🈁"), //http://www.emojibase.com/emoji/1f201/squaredkatakanakoko
    SQUARED_NG("🆖"), //http://www.emojibase.com/emoji/1f196/squaredng

    //Row#: 17
    SQUARED_OK("🆗"), //http://www.emojibase.com/emoji/1f197/squaredok
    SQUARED_EXCLAMATION_MARK("🆙"), //http://www.emojibase.com/emoji/1f199/squaredupwithexclamationmark
    SQUARED_COOL("🆒"), //http://www.emojibase.com/emoji/1f192/squaredcool
    SQUARED_NEW("🆕"), //http://www.emojibase.com/emoji/1f195/squarednew
    SQUARED_FREE("🆓"), //http://www.emojibase.com/emoji/1f193/squaredfree
    KEYCAP_DIGIT_ZERO("0⃣"), //http://www.emojibase.com/emoji/0030-20e3/keycapdigitzero
    KEYCAP_DIGIT_ONE("1⃣"), //http://www.emojibase.com/emoji/0031-20e3/keycapdigitone
    KEYCAP_DIGIT_TWO("2⃣"), //http://www.emojibase.com/emoji/0032-20e3/keycapdigittwo

    //Row#: 18
    KEYCAP_DIGIT_THREE("3⃣"), //http://www.emojibase.com/emoji/0033-20e3/keycapdigitthree
    KEYCAP_DIGIT_FOUR("4⃣"), //http://www.emojibase.com/emoji/0034-20e3/keycapdigitfour
    KEYCAP_DIGIT_FIVE("5⃣"), //http://www.emojibase.com/emoji/0035-20e3/keycapdigitfive
    KEYCAP_DIGIT_SIX("6⃣"), //http://www.emojibase.com/emoji/0036-20e3/keycapdigitsix
    KEYCAP_DIGIT_SEVEN("7⃣"), //http://www.emojibase.com/emoji/0037-20e3/keycapdigitseven
    KEYCAP_DIGIT_EIGHT("8⃣"), //http://www.emojibase.com/emoji/0038-20e3/keycapdigiteight
    KEYCAP_DIGIT_NINE("9⃣"), //http://www.emojibase.com/emoji/0039-20e3/keycapdigitnine
    KEYCAP_TEN("🔟"), //http://www.emojibase.com/emoji/1f51f/keycapten

    //Row#: 19
    INPUT_SYMBOL_FOR_NUMBERS("🔢"), //http://www.emojibase.com/emoji/1f522/inputsymbolfornumbers
    BLACK_RIGHT_POINTING_TRIANGLE("▶"), //http://www.emojibase.com/emoji/25b6/blackrightpointingtriangle
    DOUBLE_VERTICAL_BAR("⏸"), //http://www.emojibase.com/emoji/23f8/doubleverticalbar
    BLK_RGT_POINT_TRIANGLE_DBL_VERTICAL_BAR("⏯"), //http://www.emojibase.com/emoji/23ef/blackrightpointingtrianglewithdoubleverticalbar
    BLACK_SQUARE_FOR_STOP("⏹"), //http://www.emojibase.com/emoji/23f9/blacksquareforstop
    BLACK_CIRCLE_FOR_RECORD("⏺"), //http://www.emojibase.com/emoji/23fa/blackcircleforrecord
    BLK_RGT_POINT_DBL_TRIANGLE_VERTICAL_BAR("⏭"), //http://www.emojibase.com/emoji/23ed/blackrightpointingdoubletrianglewithverticalbar
    BLK_LFT_POINT_DBL_TRIANGLE_VERTICAL_BAR("⏮"), //http://www.emojibase.com/emoji/23ee/blackleftpointingdoubletrianglewithverticalbar

    //Row#: 20
    BLK_RGT_POINT_DBL_TRIANGLE("⏩"), //http://www.emojibase.com/emoji/23e9/blackrightpointingdoubletriangle
    BLK_LFT_POINT_DBL_TRIANGLE("⏪"), //http://www.emojibase.com/emoji/23ea/blackleftpointingdoubletriangle
    TWISTED_RIGHTWARDS_ARROWS("🔀"), //http://www.emojibase.com/emoji/1f500/twistedrightwardsarrows
    CWISE_RGT_LFT_OPEN_CIRCLE_ARROW("🔁"), //http://www.emojibase.com/emoji/1f501/clockwiserightwardsandleftwardsopencirclearrows
    CWISE_RGT_LFT_OPEN_CIRCLE_ARROW_OVERLAY("🔂"), //http://www.emojibase.com/emoji/1f502/clockwiserightwardsandleftwardsopencirclearrowswithcircledoneoverlay
    BLK_LFT_POINT_TRIANGLE("◀"), //http://www.emojibase.com/emoji/25c0/blackleftpointingtriangle
    UP_POINT_SMALL_RED_TRIANGLE("🔼"), //http://www.emojibase.com/emoji/1f53c/uppointingsmallredtriangle
    DOWN_POINT_SMALL_RED_TRIANGLE("🔽"), //http://www.emojibase.com/emoji/1f53d/downpointingsmallredtriangle

    //Row#: 21
    BLK_UP_POINT_DOUBLE_TRIANGLE("⏫"), //http://www.emojibase.com/emoji/23eb/blackuppointingdoubletriangle
    BLK_DOWN_POINT_DOUBLE_TRIANGLE("⏬"), //http://www.emojibase.com/emoji/23ec/blackdownpointingdoubletriangle
    BLACK_RIGHTWARDS_ARROW("➡"), //http://www.emojibase.com/emoji/27a1/blackrightwardsarrow
    LEFTWARDS_BLACK_ARROW("⬅"), //http://www.emojibase.com/emoji/2b05/leftwardsblackarrow
    UPWARDS_BLACK_ARROW("⬆"), //http://www.emojibase.com/emoji/2b06/upwardsblackarrow
    DOWNWARDS_BLACK_ARROW("⬇"), //http://www.emojibase.com/emoji/2b07/downwardsblackarrow
    NORTHEAST_ARROW("↗"), //http://www.emojibase.com/emoji/2197/northeastarrow
    SOUTHEAST_ARROW("↘"), //http://www.emojibase.com/emoji/2198/southeastarrow

    //Row#: 22
    SOUTH_WEST_ARROW("↙"), //http://www.emojibase.com/emoji/2199/southwestarrow
    NORTH_WEST_ARROW("↖"), //http://www.emojibase.com/emoji/2196/northwestarrow
    UP_DOWN_ARROW("↕"), //http://www.emojibase.com/emoji/2195/updownarrow
    LEFT_RIGHT_ARROW("↔"), //http://www.emojibase.com/emoji/2194/leftrightarrow
    ACWISE_DOWN_UP_OPEN_CIRCLE_ARROW("🔄"), //http://www.emojibase.com/emoji/1f504/anticlockwisedownwardsandupwardsopencirclearrows
    RIGHTWARDS_ARROW_WITH_HOOK("↪"), //http://www.emojibase.com/emoji/21aa/rightwardsarrowwithhook
    LEFTWARDS_ARROW_WITH_HOOK("↩"), //http://www.emojibase.com/emoji/21a9/leftwardsarrowwithhook
    ARROW_POINT_RGT_THEN_CURVING_UP("⤴"), //http://www.emojibase.com/emoji/2934/arrowpointingrightwardsthencurvingupwards

    //Row#: 23
    ARROW_POINT_RGT_THEN_CURVING_DOWN("⤵"), //http://www.emojibase.com/emoji/2935/arrowpointingrightwardsthencurvingdownwards
    KEYCAP_NUMBER_SIGN("#⃣"), //http://www.emojibase.com/emoji/0023-20e3/keycapnumbersign
    KEYCAP_ASTERISK("*⃣"), //http://www.emojibase.com/emoji/002a-20e3/keycapasterisk
    INFORMATION_SOURCE("ℹ"), //http://www.emojibase.com/emoji/2139/informationsource
    INPUT_SYMBOL_FOR_LATIN_LETTERS("🔤"), //http://www.emojibase.com/emoji/1f524/inputsymbolforlatinletters
    INPUT_SYMBOL_LATIN_SMALL_LETTERS("🔡"), //http://www.emojibase.com/emoji/1f521/inputsymbolforlatinsmallletters
    INPUT_SYMBOL_LATIN_CAPITAL_LETTERS("🔠"), //http://www.emojibase.com/emoji/1f520/inputsymbolforlatincapitalletters
    INPUT_SYMBOL_SYMBOLS("🔣"), //http://www.emojibase.com/emoji/1f523/inputsymbolforsymbols

    //Row#: 24
    MUSICAL_NOTE("🎵"), //http://www.emojibase.com/emoji/1f3b5/musicalnote
    MULTIPLE_MUSICAL_NOTES("🎶"), //http://www.emojibase.com/emoji/1f3b6/multiplemusicalnotes
    WAVY_DASH("〰"), //http://www.emojibase.com/emoji/3030/wavydash
    CURLY_LOOP("➰"), //http://www.emojibase.com/emoji/27b0/curlyloop
    HEAVY_CHECK_MARK("✔"), //http://www.emojibase.com/emoji/2714/heavycheckmark
    CWISE_DOWN_UP_OPEN_CIRCLE_ARROWS("🔃"), //http://www.emojibase.com/emoji/1f503/clockwisedownwardsandupwardsopencirclearrows
    HEAVY_PLUS_SIGN("➕"), //http://www.emojibase.com/emoji/2795/heavyplussign
    HEAVY_MINUS_SIGN("➖"), //http://www.emojibase.com/emoji/2796/heavyminussign

    //Row#: 25
    HEAVY_DIVISION_SIGN("➗"), //http://www.emojibase.com/emoji/2797/heavydivisionsign
    HEAVY_MULTIPLICATION_X("✖"), //http://www.emojibase.com/emoji/2716/heavymultiplicationx
    HEAVY_DOLLAR_SIGN("💲"), //http://www.emojibase.com/emoji/1f4b2/heavydollarsign
    CURRENCY_EXCHANGE("💱"), //http://www.emojibase.com/emoji/1f4b1/currencyexchange
    COPYRIGHT_SIGN("©"), //http://www.emojibase.com/emoji/00a9/copyrightsign
    REGISTERED_SIGN("®"), //http://www.emojibase.com/emoji/00ae/registeredsign
    TRADEMARK_SIGN("™"), //http://www.emojibase.com/emoji/2122/trademarksign
    END_WITH_LFT_ARROW_ABOVE("🔚"), //http://www.emojibase.com/emoji/1f51a/endwithleftwardsarrowabove

    //Row#: 26
    BACK_WITH_LFT_ARROW_ABOVE("🔙"), //http://www.emojibase.com/emoji/1f519/backwithleftwardsarrowabove
    ON_EXCLAMATION_LFT_RGT_ARROW("🔛"), //http://www.emojibase.com/emoji/1f51b/onwithexclamationmarkwithleftrightarrowabove
    TOP_WITH_UP_ARROW_ABOVE("🔝"), //http://www.emojibase.com/emoji/1f51d/topwithupwardsarrowabove
    SOON_RIGHT_ARROW_ABOVE("🔜"), //http://www.emojibase.com/emoji/1f51c/soonwithrightwardsarrowabove
    BALLOT_BOX_WITH_CHECK("☑"), //http://www.emojibase.com/emoji/2611/ballotboxwithcheck
    RADIO_BUTTON("🔘"), //http://www.emojibase.com/emoji/1f518/radiobutton
    MEDIUM_WHITE_CIRCLE("⚪"), //http://www.emojibase.com/emoji/26aa/mediumwhitecircle
    MEDIUM_BLACK_CIRCLE("⚫"), //http://www.emojibase.com/emoji/26ab/mediumblackcircle

    //Row#: 27
    LARGE_GREEN_CIRCLE("\uD83D\uDD35"),
    LARGE_RED_CIRCLE("🔴"), //http://www.emojibase.com/emoji/1f534/largeredcircle
    LARGE_BLUE_CIRCLE("🔵"), //http://www.emojibase.com/emoji/1f535/largebluecircle
    SMALL_ORANGE_DIAMOND("🔸"), //http://www.emojibase.com/emoji/1f538/smallorangediamond
    SMALL_BLUE_DIAMOND("🔹"), //http://www.emojibase.com/emoji/1f539/smallbluediamond
    LARGE_ORANGE_DIAMOND("🔶"), //http://www.emojibase.com/emoji/1f536/largeorangediamond
    LARGE_BLUE_DIAMOND("🔷"), //http://www.emojibase.com/emoji/1f537/largebluediamond
    UP_POINT_RED_TRIANGLE("🔺"), //http://www.emojibase.com/emoji/1f53a/uppointingredtriangle
    BLACK_SMALL_SQUARE("▪"), //http://www.emojibase.com/emoji/25aa/blacksmallsquare

    //Row#: 28
    WHITE_SMALL_SQUARE("▫"), //http://www.emojibase.com/emoji/25ab/whitesmallsquare
    BLACK_LARGE_SQUARE("⬛"), //http://www.emojibase.com/emoji/2b1b/blacklargesquare
    WHITE_LARGE_SQUARE("⬜"), //http://www.emojibase.com/emoji/2b1c/whitelargesquare
    DOWN_POINT_RED_TRIANGLE("🔻"), //http://www.emojibase.com/emoji/1f53b/downpointingredtriangle
    BLACK_MEDIUM_SQUARE("◼"), //http://www.emojibase.com/emoji/25fc/blackmediumsquare
    WHITE_MEDIUM_SQUARE("◻"), //http://www.emojibase.com/emoji/25fb/whitemediumsquare
    BLACK_MEDIUM_SMALL_SQUARE("◾"), //http://www.emojibase.com/emoji/25fe/blackmediumsmallsquare
    WHITE_MEDIUM_SMALL_SQUARE("◽"), //http://www.emojibase.com/emoji/25fd/whitemediumsmallsquare

    //Row#: 29
    BLACK_SQUARE_BUTTON("🔲"), //http://www.emojibase.com/emoji/1f532/blacksquarebutton
    WHITE_SQUARE_BUTTON("🔳"), //http://www.emojibase.com/emoji/1f533/whitesquarebutton
    SPEAKER("🔈"), //http://www.emojibase.com/emoji/1f508/speaker
    SPEAKER_ONE_SOUND_WAVE("🔉"), //http://www.emojibase.com/emoji/1f509/speakerwithonesoundwave
    SPEAKER_THREE_SOUND_WAVES("🔊"), //http://www.emojibase.com/emoji/1f50a/speakerwiththreesoundwaves
    SPEAKER_CANCELLATION_STROKE("🔇"), //http://www.emojibase.com/emoji/1f507/speakerwithcancellationstroke
    CHEERING_MEGAPHONE("📣"), //http://www.emojibase.com/emoji/1f4e3/cheeringmegaphone
    PUBLIC_ADDRESS_LOUDSPEAKER("📢"), //http://www.emojibase.com/emoji/1f4e2/publicaddressloudspeaker

    //Row#: 30
    BELL("🔔"), //http://www.emojibase.com/emoji/1f514/bell
    BELL_WITH_CANCELLATION_STROKE("🔕"), //http://www.emojibase.com/emoji/1f515/bellwithcancellationstroke
    PLAYING_CARD_BLACK_JOKER("🃏"), //http://www.emojibase.com/emoji/1f0cf/playingcardblackjoker
    MAHJONG_TILE_RED_DRAGON("🀄"), //http://www.emojibase.com/emoji/1f004/mahjongtilereddragon
    BLACK_SPADE_SUIT("♠"), //http://www.emojibase.com/emoji/2660/blackspadesuit
    BLACK_CLUB_SUIT("♣"), //http://www.emojibase.com/emoji/2663/blackclubsuit
    BLACK_HEART_SUIT("♥"), //http://www.emojibase.com/emoji/2665/blackheartsuit
    BLACK_DIAMOND_SUIT("♦"), //http://www.emojibase.com/emoji/2666/blackdiamondsuit

    //Row#: 31
    FLOWER_PLAYING_CARDS("🎴"), //http://www.emojibase.com/emoji/1f3b4/flowerplayingcards
    EYE_IN_SPEECH_BUBBLE("👁‍🗨"), //http://www.emojipedia.org/eye-in-speech-bubble/
    THOUGHT_BALLOON("💭"), //http://www.emojibase.com/emoji/1f4ad/thoughtballoon
    RIGHT_ANGER_BUBBLE("🗯"), //http://www.emojibase.com/emoji/1f5ef/rightangerbubble
    SPEECH_BALLOON("💬"), //http://www.emojibase.com/emoji/1f4ac/speechballoon
    CLOCK_FACE_ONE_O_CLOCK("🕐"), //http://www.emojibase.com/emoji/1f550/clockfaceoneoclock
    CLOCK_FACE_TWO_O_CLOCK("🕑"), //http://www.emojibase.com/emoji/1f551/clockfacetwooclock
    CLOCK_FACE_THREE_O_CLOCK("🕒"), //http://www.emojibase.com/emoji/1f552/clockfacethreeoclock

    //Row#: 32
    CLOCK_FACE_FOUR_O_CLOCK("🕓"), //http://www.emojibase.com/emoji/1f553/clockfacefouroclock
    CLOCK_FACE_FIVE_O_CLOCK("🕔"), //http://www.emojibase.com/emoji/1f554/clockfacefiveoclock
    CLOCK_FACE_SIX_O_CLOCK("🕕"), //http://www.emojibase.com/emoji/1f555/clockfacesixoclock
    CLOCK_FACE_SEVEN_O_CLOCK("🕖"), //http://www.emojibase.com/emoji/1f556/clockfacesevenoclock
    CLOCK_FACE_EIGHT_O_CLOCK("🕗"), //http://www.emojibase.com/emoji/1f557/clockfaceeightoclock
    CLOCK_FACE_NINE_O_CLOCK("🕘"), //http://www.emojibase.com/emoji/1f558/clockfacenineoclock
    CLOCK_FACE_TEN_O_CLOCK("🕙"), //http://www.emojibase.com/emoji/1f559/clockfacetenoclock
    CLOCK_FACE_ELEVEN_O_CLOCK("🕚"), //http://www.emojibase.com/emoji/1f55a/clockfaceelevenoclock

    //Row#: 33
    CLOCK_FACE_TWELVE_O_CLOCK("🕛"), //http://www.emojibase.com/emoji/1f55b/clockfacetwelveoclock
    CLOCK_FACE_ONE_THIRTY("🕜"), //http://www.emojibase.com/emoji/1f55c/clockfaceonethirty
    CLOCK_FACE_TWO_THIRTY("🕝"), //http://www.emojibase.com/emoji/1f55d/clockfacetwothirty
    CLOCK_FACE_THREE_THIRTY("🕞"), //http://www.emojibase.com/emoji/1f55e/clockfacethreethirty
    CLOCK_FACE_FOUR_THIRTY("🕟"), //http://www.emojibase.com/emoji/1f55f/clockfacefourthirty
    CLOCK_FACE_FIVE_THIRTY("🕠"), //http://www.emojibase.com/emoji/1f560/clockfacefivethirty
    CLOCK_FACE_SIX_THIRTY("🕡"), //http://www.emojibase.com/emoji/1f561/clockfacesixthirty
    CLOCK_FACE_SEVEN_THIRTY("🕢"), //http://www.emojibase.com/emoji/1f562/clockfaceseventhirty

    //Row#: 34
    CLOCK_FACE_EIGHT_THIRTY("🕣"), //http://www.emojibase.com/emoji/1f563/clockfaceeightthirty
    CLOCK_FACE_NINE_THIRTY("🕤"), //http://www.emojibase.com/emoji/1f564/clockfaceninethirty
    CLOCK_FACE_TEN_THIRTY("🕥"), //http://www.emojibase.com/emoji/1f565/clockfacetenthirty
    CLOCK_FACE_ELEVEN_THIRTY("🕦"), //http://www.emojibase.com/emoji/1f566/clockfaceeleventhirty
    CLOCK_FACE_TWELVE_THIRTY("🕧"), //http://www.emojibase.com/emoji/1f567/clockfacetwelvethirty

    //Row#: 1
    GRINNING_FACE("😀"), //https://www.emojibase.com/emoji/1f600/grinningface
    GRIMACING_FACE("😬"), //https://www.emojibase.com/emoji/1f62c/grimacingface
    GRIMACING_FACE_WITH_SMILE_EYES("😁"), //https://www.emojibase.com/emoji/1f601/grinningfacewithsmilingeyes
    FACE_WITH_TEAR_OF_JOY("😂"), //https://www.emojibase.com/emoji/1f602/facewithtearsofjoy
    SMILING_FACE_WITH_OPEN_MOUTH("😃"), //https://www.emojibase.com/emoji/1f603/smilingfacewithopenmouth
    SMILING_FACE_WITH_OPEN_MOUTH_EYES("😄"), //https://www.emojibase.com/emoji/1f604/smilingfacewithopenmouthandsmilingeyes
    SMILING_FACE_WITH_OPEN_MOUTH_COLD_SWEAT("😅"), //https://www.emojibase.com/emoji/1f605/smilingfacewithopenmouthandcoldsweat
    SMILING_FACE_WITH_OPEN_MOUTH_HAND_TIGHT("😆"), //https://www.emojibase.com/emoji/1f606/smilingfacewithopenmouthandtightlyclosedeyes

    //Row#: 2
    SMILING_FACE_WITH_HALO("😇"), //https://www.emojibase.com/emoji/1f607/smilingfacewithhalo
    WINKING_FACE("😉"), //https://www.emojibase.com/emoji/1f609/winkingface
    BLACK_SMILING_FACE("😊"), //http://emojipedia.org/smiling-face-with-smiling-eyes/
    SLIGHTLY_SMILING_FACE("🙂"), //https://www.emojibase.com/emoji/1f642/slightlysmilingface
    UPSIDE_DOWN_FACE("🙃"), //http://emojipedia.org/upside-down-face/
    WHITE_SMILING_FACE("☺"), //https://www.emojibase.com/emoji/263a/whitesmilingface
    FACE_SAVOURING_DELICIOUS_FOOD("😋"), //https://www.emojibase.com/emoji/1f60b/facesavouringdeliciousfood
    RELIEVED_FACE("😌"), //https://www.emojibase.com/emoji/1f60c/relievedface

    //Row#: 3
    SMILING_FACE_HEART_EYES("😍"), //https://www.emojibase.com/emoji/1f60d/smilingfacewithheartshapedeyes
    FACE_THROWING_KISS("😘"), //https://www.emojibase.com/emoji/1f618/facethrowingakiss
    KISSING_FACE("😗"), //https://www.emojibase.com/emoji/1f617/kissingface
    KISSING_FACE_WITH_SMILE_EYES("😙"), //https://www.emojibase.com/emoji/1f619/kissingfacewithsmilingeyes
    KISSING_FACE_WITH_CLOSED_EYES("😚"), //https://www.emojibase.com/emoji/1f61a/kissingfacewithclosedeyes
    FACE_WITH_TONGUE_WINK_EYE("😜"), //https://www.emojibase.com/emoji/1f61c/facewithstuckouttongueandwinkingeye
    FACE_WITH_TONGUE_CLOSED_EYE("😝"), //https://www.emojibase.com/emoji/1f61d/facewithstuckouttongueandtightlyclosedeyes
    FACE_WITH_STUCK_OUT_TONGUE("😛"), //https://www.emojibase.com/emoji/1f61b/facewithstuckouttongue

    //Row#: 4
    MONEY_MOUTH_FACE("🤑"), //http://emojipedia.org/money-mouth-face/
    NERD_FACE("🤓"), //http://emojipedia.org/nerd-face/
    SMILING_FACE_WITH_SUN_GLASS("😎"), //https://www.emojibase.com/emoji/1f60e/smilingfacewithsunglasses
    HUGGING_FACE("🤗"), //http://emojipedia.org/hugging-face/
    SMIRKING_FACE("😏"), //https://www.emojibase.com/emoji/1f60f/smirkingface
    FACE_WITHOUT_MOUTH("😶"), //https://www.emojibase.com/emoji/1f636/facewithoutmouth
    NEUTRAL_FACE("😐"), //https://www.emojibase.com/emoji/1f610/neutralface
    EXPRESSIONLESS_FACE("😑"), //https://www.emojibase.com/emoji/1f611/expressionlessface

    //Row#: 5
    UNAMUSED_FACE("😒"), //https://www.emojibase.com/emoji/1f612/unamusedface
    FACE_WITH_ROLLING_EYES("🙄"), //http://emojipedia.org/face-with-rolling-eyes/
    THINKING_FACE("🤔"), //http://emojipedia.org/thinking-face/
    FLUSHED_FACE("😳"), //https://www.emojibase.com/emoji/1f633/flushedface
    DISAPPOINTED_FACE("😞"), //https://www.emojibase.com/emoji/1f61e/disappointedface
    WORRIED_FACE("😟"), //https://www.emojibase.com/emoji/1f61f/worriedface
    ANGRY_FACE("😠"), //https://www.emojibase.com/emoji/1f620/angryface
    POUTING_FACE("😡"), //https://www.emojibase.com/emoji/1f621/poutingface

    //Row#: 6
    PENSIVE_FACE("😔"), //https://www.emojibase.com/emoji/1f614/pensiveface
    CONFUSED_FACE("😕"), //https://www.emojibase.com/emoji/1f615/confusedface
    SLIGHTLY_FROWNING_FACE("🙁"), //https://www.emojibase.com/emoji/1f641/slightlyfrowningface
    WHITE_FROWNING_FACE("☹"), //https://www.emojibase.com/emoji/2639/whitefrowningface
    PERSEVERING_FACE("😣"), //https://www.emojibase.com/emoji/1f623/perseveringface
    CONFOUNDED_FACE("😖"), //https://www.emojibase.com/emoji/1f616/confoundedface
    TIRED_FACE("😫"), //https://www.emojibase.com/emoji/1f62b/tiredface
    WEARY_FACE("😩"), //https://www.emojibase.com/emoji/1f629/wearyface

    //Row#: 7
    FACE_WITH_LOOK_OF_TRIUMPH("😤"), //https://www.emojibase.com/emoji/1f624/facewithlookoftriumph
    FACE_WITH_OPEN_MOUTH("😮"), //https://www.emojibase.com/emoji/1f62e/facewithopenmouth
    FACE_SCREAMING_IN_FEAR("😱"), //https://www.emojibase.com/emoji/1f631/facescreaminginfear
    FEARFUL_FACE("😨"), //https://www.emojibase.com/emoji/1f628/fearfulface
    FACE_WITH_OPEN_MOUTH_COLD_SWEAT("😰"), //https://www.emojibase.com/emoji/1f630/facewithopenmouthandcoldsweat
    HUSHED_FACE("😯"), //https://www.emojibase.com/emoji/1f62f/hushedface
    FROWNING_FACE_WITH_OPEN_MOUTH("😦"), //https://www.emojibase.com/emoji/1f626/frowningfacewithopenmouth
    ANGUISHED_FACE("😧"), //https://www.emojibase.com/emoji/1f627/anguishedface

    //Row#: 8
    CRYING_FACE("😢"), //https://www.emojibase.com/emoji/1f622/cryingface
    DISAPPOINTED_BUT_RELIEVED_FACE("😥"), //https://www.emojibase.com/emoji/1f625/disappointedbutrelievedface
    SLEEPY_FACE("😪"), //https://www.emojibase.com/emoji/1f62a/sleepyface
    FACE_WITH_COLD_SWEAT("😓"), //https://www.emojibase.com/emoji/1f613/facewithcoldsweat
    LOUDLY_CRYING_FACE("😭"), //https://www.emojibase.com/emoji/1f62d/loudlycryingface
    DIZZY_FACE("😵"), //https://www.emojibase.com/emoji/1f635/dizzyface
    ASTONISHED_FACE("😲"), //https://www.emojibase.com/emoji/1f632/astonishedface
    ZIPPER_MOUTH_FACE("🤐"), //http://emojipedia.org/zipper-mouth-face/

    //Row#: 9
    FACE_WITH_MEDICAL_MASK("😷"), //https://www.emojibase.com/emoji/1f637/facewithmedicalmask
    FACE_WITH_THERMOMETER("🤒"), //http://emojipedia.org/face-with-thermometer/
    FACE_WITH_HEAD_BANDAGE("🤕"), //http://emojipedia.org/face-with-head-bandage/
    SLEEPING_FACE("😴"), //https://www.emojibase.com/emoji/1f634/sleepingface
    SLEEPING_SYMBOL("💤"), //https://www.emojibase.com/emoji/1f4a4/sleepingsymbol
    PILE_OF_POO("💩"), //https://www.emojibase.com/emoji/1f4a9/pileofpoo
    SMILING_FACE_WITH_HORNS("😈"), //https://www.emojibase.com/emoji/1f608/smilingfacewithhorns
    IMP("👿"), //https://www.emojibase.com/emoji/1f47f/imp

    //Row#: 10
    JAPANESE_OGRE("👹"), //https://www.emojibase.com/emoji/1f479/japaneseogre
    JAPANESE_GOBLIN("👺"), //https://www.emojibase.com/emoji/1f47a/japanesegoblin
    SKULL("💀"), //https://www.emojibase.com/emoji/1f480/skull
    GHOST("👻"), //https://www.emojibase.com/emoji/1f47b/ghost
    EXTRA_TERRESTRIAL_ALIEN("👽"), //https://www.emojibase.com/emoji/1f47d/extraterrestrialalien
    ROBOT_FACE("🤖"), //http://emojipedia.org/robot-face/
    SMILING_CAT_FACE_OPEN_MOUTH("😺"), //https://www.emojibase.com/emoji/1f63a/smilingcatfacewithopenmouth
    GRINNING_CAT_FACE_SMILE_EYES("😸"), //https://www.emojibase.com/emoji/1f638/grinningcatfacewithsmilingeyes

    //Row#: 11
    CAT_FACE_TEARS_OF_JOY("😹"), //https://www.emojibase.com/emoji/1f639/catfacewithtearsofjoy
    SMILING_CAT_FACE_HEART_SHAPED_EYES("😻"), //https://www.emojibase.com/emoji/1f63b/smilingcatfacewithheartshapedeyes
    CAT_FACE_WRY_SMILE("😼"), //https://www.emojibase.com/emoji/1f63c/catfacewithwrysmile
    KISSING_CAT_FACE_CLOSED_EYES("😽"), //https://www.emojibase.com/emoji/1f63d/kissingcatfacewithclosedeyes
    WEARY_CAT_FACE("🙀"), //https://www.emojibase.com/emoji/1f640/wearycatface
    CRYING_CAT_FACE("😿"), //https://www.emojibase.com/emoji/1f63f/cryingcatface
    POUTING_CAT_FACE("😾"), //https://www.emojibase.com/emoji/1f63e/poutingcatface
    PERSON_BOTH_HAND_CELEBRATION("🙌"), //https://www.emojibase.com/emoji/1f64c/personraisingbothhandsincelebration //http://emojipedia.org/person-raising-both-hands-in-celebration/
    PERSON_BOTH_HAND_CELEBRATION_TYPE_1_2("🙌🏻"),
    PERSON_BOTH_HAND_CELEBRATION_TYPE_3("🙌🏼"),
    PERSON_BOTH_HAND_CELEBRATION_TYPE_4("🙌🏽"),
    PERSON_BOTH_HAND_CELEBRATION_TYPE_5("🙌🏾"),
    PERSON_BOTH_HAND_CELEBRATION_TYPE_6("🙌🏿"),

    //Row#: 12
    CLAPPING_HAND("👏"), //https://www.emojibase.com/emoji/1f44f/clappinghandssign //http://emojipedia.org/clapping-hands-sign/
    CLAPPING_HAND_TYPE_1_2("👏🏼"),
    CLAPPING_HAND_TYPE_3("👏🏼"),
    CLAPPING_HAND_TYPE_4("👏🏽"),
    CLAPPING_HAND_TYPE_5("👏🏾"),
    CLAPPING_HAND_TYPE_6("👏🏿"),
    WAVING_HANDS("👋"), //https://www.emojibase.com/emoji/1f44b/wavinghandsign //http://emojipedia.org/waving-hand-sign/
    WAVING_HANDS_TYPE_1_2("👋🏻"),
    WAVING_HANDS_TYPE_3("👋🏼"),
    WAVING_HANDS_TYPE_4("👋🏽"),
    WAVING_HANDS_TYPE_5("👋🏾"),
    WAVING_HANDS_TYPE_6("👋🏿"),
    THUMBS_UP("👍"), //https://www.emojibase.com/emoji/1f44d/thumbsupsign //http://emojipedia.org/thumbs-up-sign/
    THUMBS_UP_TYPE_1_2("👍🏻"),
    THUMBS_UP_TYPE_3("👍🏼"),
    THUMBS_UP_TYPE_4("👍🏽"),
    THUMBS_UP_TYPE_5("👍🏾"),
    THUMBS_UP_TYPE_6("👍🏿"),
    THUMBS_DOWN("👎"), //https://www.emojibase.com/emoji/1f44e/thumbsdownsign //http://emojipedia.org/thumbs-down-sign/
    THUMBS_DOWN_TYPE_1_2("👎🏻"),
    THUMBS_DOWN_TYPE_3("👎🏼"),
    THUMBS_DOWN_TYPE_4("👎🏽"),
    THUMBS_DOWN_TYPE_5("👎🏾"),
    THUMBS_DOWN_TYPE_6("👎🏿"),
    FIST_HAND("👊"), //https://www.emojibase.com/emoji/1f44a/fistedhandsign //http://emojipedia.org/fisted-hand-sign/
    FIST_HAND_TYPE_1_2("👊🏻"),
    FIST_HAND_TYPE_3("👊🏼"),
    FIST_HAND_TYPE_4("👊🏽"),
    FIST_HAND_TYPE_5("👊🏾"),
    FIST_HAND_TYPE_6("👊🏿"),
    RAISED_FIST("✊"), //https://www.emojibase.com/emoji/270a/raisedfist //http://emojipedia.org/raised-fist/
    RAISED_FIST_TYPE_1_2("✊🏻"),
    RAISED_FIST_TYPE_3("✊🏼"),
    RAISED_FIST_TYPE_4("✊🏽"),
    RAISED_FIST_TYPE_5("✊🏾"),
    RAISED_FIST_TYPE_6("✊🏿"),
    VICTORY_HAND("✌"), //https://www.emojibase.com/emoji/270c/victoryhand //http://emojipedia.org/victory-hand/
    VICTORY_HAND_TYPE_1_2("✌🏻"),
    VICTORY_HAND_TYPE_3("✌🏼"),
    VICTORY_HAND_TYPE_4("✌🏽"),
    VICTORY_HAND_TYPE_5("✌🏾"),
    VICTORY_HAND_TYPE_6("✌🏿"),
    OK_HAND("👌"), //https://www.emojibase.com/emoji/1f44c/okhandsign //http://emojipedia.org/ok-hand-sign/
    OK_HAND_TYPE_1_2("👌🏻"),
    OK_HAND_TYPE_3("👌🏼"),
    OK_HAND_TYPE_4("👌🏽"),
    OK_HAND_TYPE_5("👌🏾"),
    OK_HAND_TYPE_6("👌🏿"),

    //Row#: 13
    RAISED_HAND("✋"), //https://www.emojibase.com/emoji/270b/raisedhand //http://emojipedia.org/raised-hand/
    RAISED_HAND_TYPE_1_2("✋🏻"),
    RAISED_HAND_TYPE_3("✋🏼"),
    RAISED_HAND_TYPE_4("✋🏽"),
    RAISED_HAND_TYPE_5("✋🏾"),
    RAISED_HAND_TYPE_6("✋🏿"),
    OPEN_HAND("👐"), //https://www.emojibase.com/emoji/1f450/openhandssign //http://emojipedia.org/open-hands-sign/
    OPEN_HAND_TYPE_1_2("👐🏻"),
    OPEN_HAND_TYPE_3("👐🏼"),
    OPEN_HAND_TYPE_4("👐🏽"),
    OPEN_HAND_TYPE_5("👐🏾"),
    OPEN_HAND_TYPE_6("👐🏿"),
    FLEXED_BICEPS("💪"), //https://www.emojibase.com/emoji/1f4aa/flexedbiceps //http://emojipedia.org/flexed-biceps/
    FLEXED_BICEPS_TYPE_1_2("💪🏻"),
    FLEXED_BICEPS_TYPE_3("💪🏼"),
    FLEXED_BICEPS_TYPE_4("💪🏽"),
    FLEXED_BICEPS_TYPE_5("💪🏾"),
    FLEXED_BICEPS_TYPE_6("💪🏿"),
    FOLDED_HANDS("🙏"), //https://www.emojibase.com/emoji/1f64f/personwithfoldedhands //http://emojipedia.org/person-with-folded-hands/
    FOLDED_HANDS_TYPE_1_2("🙏🏻"),
    FOLDED_HANDS_TYPE_3("🙏🏼"),
    FOLDED_HANDS_TYPE_4("🙏🏽"),
    FOLDED_HANDS_TYPE_5("🙏🏾"),
    FOLDED_HANDS_TYPE_6("🙏🏿"),
    UP_POINTING_INDEX("☝"), //https://www.emojibase.com/emoji/261d/whiteuppointingindex //http://emojipedia.org/white-up-pointing-index/
    UP_POINTING_INDEX_TYPE_1_2("☝🏻"),
    UP_POINTING_INDEX_TYPE_3("☝🏼"),
    UP_POINTING_INDEX_TYPE_4("☝🏽"),
    UP_POINTING_INDEX_TYPE_5("☝🏾"),
    UP_POINTING_INDEX_TYPE_6("☝🏿"),
    UP_POINTING_BACKHAND_INDEX("👆"), //https://www.emojibase.com/emoji/1f446/whiteuppointingbackhandindex //http://emojipedia.org/white-up-pointing-backhand-index/
    UP_POINTING_BACKHAND_INDEX_TYPE_1_2("👆🏻"),
    UP_POINTING_BACKHAND_INDEX_TYPE_3("👆🏼"),
    UP_POINTING_BACKHAND_INDEX_TYPE_4("👆🏽"),
    UP_POINTING_BACKHAND_INDEX_TYPE_5("👆🏾"),
    UP_POINTING_BACKHAND_INDEX_TYPE_6("👆🏿"),
    DOWN_POINTING_BACKHAND_INDEX("👇"), //https://www.emojibase.com/emoji/1f447/whitedownpointingbackhandindex //http://emojipedia.org/white-down-pointing-backhand-index/
    DOWN_POINTING_BACKHAND_INDEX_TYPE_1_2("👇🏻"),
    DOWN_POINTING_BACKHAND_INDEX_TYPE_3("👇🏼"),
    DOWN_POINTING_BACKHAND_INDEX_TYPE_4("👇🏽"),
    DOWN_POINTING_BACKHAND_INDEX_TYPE_5("👇🏾"),
    DOWN_POINTING_BACKHAND_INDEX_TYPE_6("👇🏿"),
    LEFT_POINTING_BACKHAND_INDEX("👈"), //https://www.emojibase.com/emoji/1f448/whiteleftpointingbackhandindex //http://emojipedia.org/white-left-pointing-backhand-index/
    LEFT_POINTING_BACKHAND_INDEX_TYPE_1_2("👈🏻"),
    LEFT_POINTING_BACKHAND_INDEX_TYPE_3("👈🏼"),
    LEFT_POINTING_BACKHAND_INDEX_TYPE_4("👈🏽"),
    LEFT_POINTING_BACKHAND_INDEX_TYPE_5("👈🏾"),
    LEFT_POINTING_BACKHAND_INDEX_TYPE_6("👈🏿"),

    //Row#: 14
    RIGHT_POINTING_BACKHAND_INDEX("👉"), //https://www.emojibase.com/emoji/1f449/whiterightpointingbackhandindex //http://emojipedia.org/white-right-pointing-backhand-index/
    RIGHT_POINTING_BACKHAND_INDEX_TYPE_1_2("👉🏻"),
    RIGHT_POINTING_BACKHAND_INDEX_TYPE_3("👉🏼"),
    RIGHT_POINTING_BACKHAND_INDEX_TYPE_4("👉🏽"),
    RIGHT_POINTING_BACKHAND_INDEX_TYPE_5("👉🏾"),
    RIGHT_POINTING_BACKHAND_INDEX_TYPE_6("👉🏿"),
    REVERSE_MIDDLE_FINGER("🖕"), //https://www.emojibase.com/emoji/1f595/reversedhandwithmiddlefingerextended //http://emojipedia.org/reversed-hand-with-middle-finger-extended/
    REVERSE_MIDDLE_FINGER_TYPE_1_2("🖕🏻"),
    REVERSE_MIDDLE_FINGER_TYPE_3("🖕🏼"),
    REVERSE_MIDDLE_FINGER_TYPE_4("🖕🏽"),
    REVERSE_MIDDLE_FINGER_TYPE_5("🖕🏾"),
    REVERSE_MIDDLE_FINGER_TYPE_6("🖕🏿"),
    RAISED_HAND_FINGERS_SPLAYED("🖐"), //https://www.emojibase.com/emoji/1f590/raisedhandwithfingerssplayed //http://emojipedia.org/raised-hand-with-fingers-splayed/
    RAISED_HAND_FINGERS_SPLAYED_TYPE_1_2("🖐🏻"),
    RAISED_HAND_FINGERS_SPLAYED_TYPE_3("🖐🏼"),
    RAISED_HAND_FINGERS_SPLAYED_TYPE_4("🖐🏽"),
    RAISED_HAND_FINGERS_SPLAYED_TYPE_5("🖐🏾"),
    RAISED_HAND_FINGERS_SPLAYED_TYPE_6("🖐🏿"),
    SIGN_OF_HORN("🤘"), //http://emojipedia.org/sign-of-the-horns/
    SIGN_OF_HORN_TYPE_1_2("🤘🏻"),
    SIGN_OF_HORN_TYPE_3("🤘🏼"),
    SIGN_OF_HORN_TYPE_4("🤘🏽"),
    SIGN_OF_HORN_TYPE_5("🤘🏾"),
    SIGN_OF_HORN_TYPE_6("🤘🏿"),
    RAISED_HAND_PART_BETWEEN_MIDDLE_RING("🖖"), //https://www.emojibase.com/emoji/1f596/raisedhandwithpartbetweenmiddleandringfingers //http://emojipedia.org/raised-hand-with-part-between-middle-and-ring-fingers/
    RAISED_HAND_PART_BETWEEN_MIDDLE_RING_TYPE_1_2("🖖🏻"),
    RAISED_HAND_PART_BETWEEN_MIDDLE_RING_TYPE_3("🖖🏼"),
    RAISED_HAND_PART_BETWEEN_MIDDLE_RING_TYPE_4("🖖🏽"),
    RAISED_HAND_PART_BETWEEN_MIDDLE_RING_TYPE_5("🖖🏾"),
    RAISED_HAND_PART_BETWEEN_MIDDLE_RING_TYPE_6("🖖🏿"),
    WRITING_HAND("✍"), //https://www.emojibase.com/emoji/270d/writinghand //http://emojipedia.org/writing-hand/
    WRITING_HAND_TYPE_1_2("✍🏻"),
    WRITING_HAND_TYPE_3("✍🏼"),
    WRITING_HAND_TYPE_4("✍🏽"),
    WRITING_HAND_TYPE_5("✍🏾"),
    WRITING_HAND_TYPE_6("✍🏿"),
    NAIL_POLISH("💅"), //https://www.emojibase.com/emoji/1f485/nailpolish //http://emojipedia.org/nail-polish/
    NAIL_POLISH_TYPE_1_2("💅🏻"),
    NAIL_POLISH_TYPE_3("💅🏼"),
    NAIL_POLISH_TYPE_4("💅🏽"),
    NAIL_POLISH_TYPE_5("💅🏾"),
    NAIL_POLISH_TYPE_6("💅🏿"),
    MOUTH("👄"), //https://www.emojibase.com/emoji/1f444/mouth

    //Row#: 15
    TONGUE("👅"), //https://www.emojibase.com/emoji/1f445/tongue
    EAR("👂"), //https://www.emojibase.com/emoji/1f442/ear //http://emojipedia.org/ear/
    EAR_TYPE_1_2("👂🏻"),
    EAR_TYPE_3("👂🏼"),
    EAR_TYPE_4("👂🏽"),
    EAR_TYPE_5("👂🏾"),
    EAR_TYPE_6("👂🏿"),
    NOSE("👃"), //https://www.emojibase.com/emoji/1f443/nose //http://emojipedia.org/nose/
    NOSE_TYPE_1_2("👃🏻"),
    NOSE_TYPE_3("👃🏼"),
    NOSE_TYPE_4("👃🏽"),
    NOSE_TYPE_5("👃🏾"),
    NOSE_TYPE_6("👃🏿"),
    EYE("👁"), //https://www.emojibase.com/emoji/1f441/eye
    EYES("👀"), //https://www.emojibase.com/emoji/1f440/eyes
    BUST_IN_SILHOUETTE("👤"), //https://www.emojibase.com/emoji/1f464/bustinsilhouette
    BUSTS_IN_SILHOUETTE("👥"), //https://www.emojibase.com/emoji/1f465/bustsinsilhouette
    SPEAKING_HEAD_IN_SILHOUETTE("🗣"), //https://www.emojibase.com/emoji/1f5e3/speakingheadinsilhouette

    //Row#: 16
    BABY("👶"), //https://www.emojibase.com/emoji/1f476/baby //http://emojipedia.org/baby/
    BABY_TYPE_1_2("👶🏻"),
    BABY_TYPE_3("👶🏼"),
    BABY_TYPE_4("👶🏽"),
    BABY_TYPE_5("👶🏾"),
    BABY_TYPE_6("👶🏿"),
    BOY("👦"), //https://www.emojibase.com/emoji/1f466/boy //http://emojipedia.org/boy/
    BOY_TYPE_1_2("👦🏻"),
    BOY_TYPE_3("👦🏼"),
    BOY_TYPE_4("👦🏽"),
    BOY_TYPE_5("👦🏾"),
    BOY_TYPE_6("👦🏿"),
    GIRL("👧"), //https://www.emojibase.com/emoji/1f467/girl //http://emojipedia.org/girl/
    GIRL_TYPE_1_2("👧🏻"),
    GIRL_TYPE_3("👧🏼"),
    GIRL_TYPE_4("👧🏽"),
    GIRL_TYPE_5("👧🏾"),
    GIRL_TYPE_6("👧🏿"),
    MAN("👨"), //https://www.emojibase.com/emoji/1f468/man //http://emojipedia.org/man/
    MAN_TYPE_1_2("👨🏻"),
    MAN_TYPE_3("👨🏼"),
    MAN_TYPE_4("👨🏽"),
    MAN_TYPE_5("👨🏾"),
    MAN_TYPE_6("👨🏿"),
    WOMEN("👩"), //https://www.emojibase.com/emoji/1f469/woman //http://emojipedia.org/woman/
    WOMEN_TYPE_1_2("👩🏻"),
    WOMEN_TYPE_3("👩🏼"),
    WOMEN_TYPE_4("👩🏽"),
    WOMEN_TYPE_5("👩🏾"),
    WOMEN_TYPE_6("👩🏿"),
    PERSON_WITH_BLOND_HAIR("👱"), //https://www.emojibase.com/emoji/1f471/personwithblondhair //http://emojipedia.org/person-with-blond-hair/
    PERSON_WITH_BLOND_HAIR_TYPE_1_2("👱🏻"),
    PERSON_WITH_BLOND_HAIR_TYPE_3("👱🏼"),
    PERSON_WITH_BLOND_HAIR_TYPE_4("👱🏽"),
    PERSON_WITH_BLOND_HAIR_TYPE_5("👱🏾"),
    PERSON_WITH_BLOND_HAIR_TYPE_6("👱🏿"),
    OLDER_MAN("👴"), //https://www.emojibase.com/emoji/1f474/olderman //http://emojipedia.org/older-man/
    OLDER_MAN_TYPE_1_2("👴🏻"),
    OLDER_MAN_TYPE_3("👴🏼"),
    OLDER_MAN_TYPE_4("👴🏽"),
    OLDER_MAN_TYPE_5("👴🏾"),
    OLDER_MAN_TYPE_6("👴🏿"),
    OLDER_WOMEN("👵"), //https://www.emojibase.com/emoji/1f475/olderwoman //http://emojipedia.org/older-woman/
    OLDER_WOMEN_TYPE_1_2("👵🏻"),
    OLDER_WOMEN_TYPE_3("👵🏼"),
    OLDER_WOMEN_TYPE_4("👵🏽"),
    OLDER_WOMEN_TYPE_5("👵🏾"),
    OLDER_WOMEN_TYPE_6("👵🏿"),

    //Row#: 17
    MAN_WITH_GUA_PI_MAO("👲"), //https://www.emojibase.com/emoji/1f472/manwithguapimao //http://emojipedia.org/man-with-gua-pi-mao/
    MAN_WITH_GUA_PI_MAO_TYPE_1_2("👲🏼"),
    MAN_WITH_GUA_PI_MAO_TYPE_3("👲🏼"),
    MAN_WITH_GUA_PI_MAO_TYPE_4("👲🏽"),
    MAN_WITH_GUA_PI_MAO_TYPE_5("👲🏾"),
    MAN_WITH_GUA_PI_MAO_TYPE_6("👲🏿"),
    MAN_WITH_TURBAN("👳"), //https://www.emojibase.com/emoji/1f473/manwithturban //http://emojipedia.org/man-with-turban/
    MAN_WITH_TURBAN_TYPE_1_2("👳🏻"),
    MAN_WITH_TURBAN_TYPE_3("👳🏼"),
    MAN_WITH_TURBAN_TYPE_4("👳🏽"),
    MAN_WITH_TURBAN_TYPE_5("👳🏾"),
    MAN_WITH_TURBAN_TYPE_6("👳🏿"),
    POLICE_OFFICER("👮"), //https://www.emojibase.com/emoji/1f46e/policeofficer //http://emojipedia.org/police-officer/
    POLICE_OFFICER_TYPE_1_2("👮🏻"),
    POLICE_OFFICER_TYPE_3("👮🏼"),
    POLICE_OFFICER_TYPE_4("👮🏽"),
    POLICE_OFFICER_TYPE_5("👮🏾"),
    POLICE_OFFICER_TYPE_6("👮🏿"),
    CONSTRUCTION_WORKER("👷"), //https://www.emojibase.com/emoji/1f477/constructionworker //http://emojipedia.org/construction-worker/
    CONSTRUCTION_WORKER_TYPE_1_2("👷🏻"),
    CONSTRUCTION_WORKER_TYPE_3("👷🏼"),
    CONSTRUCTION_WORKER_TYPE_4("👷🏽"),
    CONSTRUCTION_WORKER_TYPE_5("👷🏾"),
    CONSTRUCTION_WORKER_TYPE_6("👷🏿"),
    GUARDS_MAN("💂"), //https://www.emojibase.com/emoji/1f482/guardsman //http://emojipedia.org/guardsman/
    GUARDS_MAN_TYPE_1_2("💂🏻"),
    GUARDS_MAN_TYPE_3("💂🏼"),
    GUARDS_MAN_TYPE_4("💂🏽"),
    GUARDS_MAN_TYPE_5("💂🏾"),
    GUARDS_MAN_TYPE_6("💂🏿"),
    SPY("🕵"), //https://www.emojibase.com/emoji/1f575/sleuthorspy
    FATHER_CHRISTMAS("🎅"), //https://www.emojibase.com/emoji/1f385/fatherchristmas //http://emojipedia.org/father-christmas/
    FATHER_CHRISTMAS_TYPE_1_2("🎅🏻"),
    FATHER_CHRISTMAS_TYPE_3("🎅🏼"),
    FATHER_CHRISTMAS_TYPE_4("🎅🏽"),
    FATHER_CHRISTMAS_TYPE_5("🎅🏾"),
    FATHER_CHRISTMAS_TYPE_6("🎅🏿"),
    BABY_ANGEL("👼"), //https://www.emojibase.com/emoji/1f47c/babyangel //http://emojipedia.org/baby-angel/
    BABY_ANGEL_TYPE_1_2("👼🏻"),
    BABY_ANGEL_TYPE_3("👼🏼"),
    BABY_ANGEL_TYPE_4("👼🏽"),
    BABY_ANGEL_TYPE_5("👼🏾"),
    BABY_ANGEL_TYPE_6("👼🏿"),

    //Row#: 18
    PRINCESS("👸"), //https://www.emojibase.com/emoji/1f478/princess //http://emojipedia.org/princess/
    PRINCESS_TYPE_1_2("👸🏻"),
    PRINCESS_TYPE_3("👸🏼"),
    PRINCESS_TYPE_4("👸🏽"),
    PRINCESS_TYPE_5("👸🏾"),
    PRINCESS_TYPE_6("👸🏿"),
    BRIDE_WITH_VEIL("👰"), //https://www.emojibase.com/emoji/1f470/bridewithveil //http://emojipedia.org/bride-with-veil/
    BRIDE_WITH_VEIL_TYPE_1_2("👰🏻"),
    BRIDE_WITH_VEIL_TYPE_3("👰🏼"),
    BRIDE_WITH_VEIL_TYPE_4("👰🏽"),
    BRIDE_WITH_VEIL_TYPE_5("👰🏾"),
    BRIDE_WITH_VEIL_TYPE_6("👰🏿"),
    PEDESTRIAN("🚶"), //https://www.emojibase.com/emoji/1f6b6/pedestrian //http://emojipedia.org/pedestrian/
    PEDESTRIAN_TYPE_1_2("🚶🏻"),
    PEDESTRIAN_TYPE_3("🚶🏼"),
    PEDESTRIAN_TYPE_4("🚶🏽"),
    PEDESTRIAN_TYPE_5("🚶🏾"),
    PEDESTRIAN_TYPE_6("🚶🏿"),
    RUNNER("🏃"), //https://www.emojibase.com/emoji/1f3c3/runner //http://emojipedia.org/runner/
    RUNNER_TYPE_1_2("🏃🏻"),
    RUNNER_TYPE_3("🏃🏼"),
    RUNNER_TYPE_4("🏃🏽"),
    RUNNER_TYPE_5("🏃🏾"),
    RUNNER_TYPE_6("🏃🏿"),
    DANCER("💃"), //https://www.emojibase.com/emoji/1f483/dancer //http://emojipedia.org/dancer/
    DANCER_TYPE_1_2("💃🏻"),
    DANCER_TYPE_3("💃🏼"),
    DANCER_TYPE_4("💃🏽"),
    DANCER_TYPE_5("💃🏾"),
    DANCER_TYPE_6("💃🏿"),
    WOMEN_WITH_BUNNY_YEARS("👯"), //https://www.emojibase.com/emoji/1f46f/womanwithbunnyears
    MAN_WOMEN_HOLDING_HANDS("👫"), //https://www.emojibase.com/emoji/1f46b/manandwomanholdinghands
    TWO_MAN_HOLDING_HANDS("👬"), //https://www.emojibase.com/emoji/1f46c/twomenholdinghands

    //Row#: 19
    TWO_WOMEN_HOLDING_HANDS("👭"), //https://www.emojibase.com/emoji/1f46d/twowomenholdinghands
    PERSON_BOWING_DEEPLY("🙇"), //https://www.emojibase.com/emoji/1f647/personbowingdeeply //http://emojipedia.org/person-bowing-deeply/
    PERSON_BOWING_DEEPLY_TYPE_1_2("🙇🏻"),
    PERSON_BOWING_DEEPLY_TYPE_3("🙇🏼"),
    PERSON_BOWING_DEEPLY_TYPE_4("🙇🏽"),
    PERSON_BOWING_DEEPLY_TYPE_5("🙇🏾"),
    PERSON_BOWING_DEEPLY_TYPE_6("🙇🏿"),
    INFORMATION_DESK_PERSON("💁"), //https://www.emojibase.com/emoji/1f481/informationdeskperson //http://emojipedia.org/information-desk-person/
    INFORMATION_DESK_PERSON_TYPE_1_2("💁🏻"),
    INFORMATION_DESK_PERSON_TYPE_3("💁🏼"),
    INFORMATION_DESK_PERSON_TYPE_4("💁🏽"),
    INFORMATION_DESK_PERSON_TYPE_5("💁🏾"),
    INFORMATION_DESK_PERSON_TYPE_6("💁🏿"),
    FACE_WITH_NO_GOOD_GESTURE("🙅"), //https://www.emojibase.com/emoji/1f645/facewithnogoodgesture //http://emojipedia.org/face-with-no-good-gesture/
    FACE_WITH_NO_GOOD_GESTURE_TYPE_1_2("🙅🏻"),
    FACE_WITH_NO_GOOD_GESTURE_TYPE_3("🙅🏼"),
    FACE_WITH_NO_GOOD_GESTURE_TYPE_4("🙅🏽"),
    FACE_WITH_NO_GOOD_GESTURE_TYPE_5("🙅🏾"),
    FACE_WITH_NO_GOOD_GESTURE_TYPE_6("🙅🏿"),
    FACE_WITH_OK_GESTURE("🙆"), //https://www.emojibase.com/emoji/1f646/facewithokgesture //http://emojipedia.org/face-with-ok-gesture/
    FACE_WITH_OK_GESTURE_TYPE_1_2("🙆🏻"),
    FACE_WITH_OK_GESTURE_TYPE_3("🙆🏼"),
    FACE_WITH_OK_GESTURE_TYPE_4("🙆🏽"),
    FACE_WITH_OK_GESTURE_TYPE_5("🙆🏾"),
    FACE_WITH_OK_GESTURE_TYPE_6("🙆🏿"),
    HAPPY_PERSON_RAISE_ONE_HAND("🙋"), //https://www.emojibase.com/emoji/1f64b/happypersonraisingonehand //http://emojipedia.org/happy-person-raising-one-hand/
    HAPPY_PERSON_RAISE_ONE_HAND_TYPE_1_2("🙋🏻"),
    HAPPY_PERSON_RAISE_ONE_HAND_TYPE_3("🙋🏼"),
    HAPPY_PERSON_RAISE_ONE_HAND_TYPE_4("🙋🏽"),
    HAPPY_PERSON_RAISE_ONE_HAND_TYPE_5("🙋🏾"),
    HAPPY_PERSON_RAISE_ONE_HAND_TYPE_6("🙋🏿"),
    PERSON_WITH_POUTING_FACE("🙎"), //https://www.emojibase.com/emoji/1f64e/personwithpoutingface //http://emojipedia.org/person-with-pouting-face/
    PERSON_WITH_POUTING_FACE_TYPE_1_2("🙎🏻"),
    PERSON_WITH_POUTING_FACE_TYPE_3("🙎🏼"),
    PERSON_WITH_POUTING_FACE_TYPE_4("🙎🏽"),
    PERSON_WITH_POUTING_FACE_TYPE_5("🙎🏾"),
    PERSON_WITH_POUTING_FACE_TYPE_6("🙎🏿"),
    PERSON_FROWNING("🙍"), //https://www.emojibase.com/emoji/1f64d/personfrowning //http://emojipedia.org/person-frowning/
    PERSON_FROWNING_TYPE_1_2("🙍🏻"),
    PERSON_FROWNING_TYPE_3("🙍🏼"),
    PERSON_FROWNING_TYPE_4("🙍🏽"),
    PERSON_FROWNING_TYPE_5("🙍🏾"),
    PERSON_FROWNING_TYPE_6("🙍🏿"),

    //Row#: 20
    HAIRCUT("💇"), //https://www.emojibase.com/emoji/1f487/haircut //http://emojipedia.org/haircut/
    HAIRCUT_TYPE_1_2("💇🏻"),
    HAIRCUT_TYPE_3("💇🏼"),
    HAIRCUT_TYPE_4("💇🏽"),
    HAIRCUT_TYPE_5("💇🏾"),
    HAIRCUT_TYPE_6("💇🏿"),
    FACE_MASSAGE("💆"), //https://www.emojibase.com/emoji/1f486/facemassage //http://emojipedia.org/face-massage/
    FACE_MASSAGE_TYPE_1_2("💆🏻"),
    FACE_MASSAGE_TYPE_3("💆🏻"),
    FACE_MASSAGE_TYPE_4("💆🏽"),
    FACE_MASSAGE_TYPE_5("💆🏾"),
    FACE_MASSAGE_TYPE_6("💆🏿"),
    COUPLE_WITH_HEART("💑"), //https://www.emojibase.com/emoji/1f491/couplewithheart
    COUPLE_WITH_HEART_WOMAN("👩‍❤️‍👩"), //http://emojipedia.org/couple-with-heart-woman-woman/
    COUPLE_WITH_HEART_MAN("👨‍❤️‍👨"), //http://emojipedia.org/couple-with-heart-man-man/
    KISS("💏"), //https://www.emojibase.com/emoji/1f48f/kiss
    KISS_WOMAN("👩‍❤️‍💋‍👩"), //http://emojipedia.org/kiss-woman-woman/
    KISS_MAN("👨‍❤️‍💋‍👨"), //http://emojipedia.org/kiss-man-man/

    //Row#: 21
    FAMILY("👪"), //https://www.emojibase.com/emoji/1f46a/family
    FAMILY_MAN_WOMEN_GIRL("👨‍👩‍👧"), //http://emojipedia.org/family-man-woman-girl/
    FAMILY_MAN_WOMEN_GIRL_BOY("👨‍👩‍👧‍👦"), //http://emojipedia.org/family-man-woman-girl-boy/
    FAMILY_MAN_WOMEN_BOY_BOY("👨‍👩‍👦‍👦"), //http://emojipedia.org/family-man-woman-boy-boy/
    FAMILY_MAN_WOMEN_GIRL_GIRL("👨‍👩‍👧‍👧"), //http://emojipedia.org/family-man-woman-girl-girl/
    FAMILY_WOMAN_WOMEN_BOY("👩‍👩‍👦"), //http://emojipedia.org/family-woman-woman-boy/
    FAMILY_WOMAN_WOMEN_GIRL("👩‍👩‍👧"), //http://emojipedia.org/family-woman-woman-girl/
    FAMILY_WOMAN_WOMEN_GIRL_BOY("👩‍👩‍👧‍👦"), //http://emojipedia.org/family-woman-woman-girl-boy/

    //Row#: 22
    FAMILY_WOMAN_WOMEN_BOY_BOY("👩‍👩‍👦‍👦"), //http://emojipedia.org/family-woman-woman-boy-boy/
    FAMILY_WOMAN_WOMEN_GIRL_GIRL("👩‍👩‍👧‍👧"), //http://emojipedia.org/family-woman-woman-girl-girl/
    FAMILY_MAN_MAN_BOY("👨‍👨‍👦"), //http://emojipedia.org/family-man-man-boy/
    FAMILY_MAN_MAN_GIRL("👨‍👨‍👧"), //http://emojipedia.org/family-man-man-girl/
    FAMILY_MAN_MAN_GIRL_BOY("👨‍👨‍👧‍👦"), //http://emojipedia.org/family-man-man-girl-boy/
    FAMILY_MAN_MAN_BOY_BOY("👨‍👨‍👦‍👦"), //http://emojipedia.org/family-man-man-boy-boy/
    FAMILY_MAN_MAN_GIRL_GIRL("👨‍👨‍👧‍👧"), //http://emojipedia.org/family-man-man-girl-girl/
    WOMAN_CLOTHES("👚"), //https://www.emojibase.com/emoji/1f45a/womansclothes

    //Row#: 23
    T_SHIRT("👕"), //https://www.emojibase.com/emoji/1f455/tshirt
    JEANS("👖"), //https://www.emojibase.com/emoji/1f456/jeans
    NECKTIE("👔"), //https://www.emojibase.com/emoji/1f454/necktie
    DRESS("👗"), //https://www.emojibase.com/emoji/1f457/dress
    BIKINI("👙"), //https://www.emojibase.com/emoji/1f459/bikini
    KIMONO("👘"), //https://www.emojibase.com/emoji/1f458/kimono
    LIPSTICK("💄"), //https://www.emojibase.com/emoji/1f484/lipstick
    KISS_MARK("💋"), //https://www.emojibase.com/emoji/1f48b/kissmark

    //Row#: 24
    FOOTPRINTS("👣"), //https://www.emojibase.com/emoji/1f463/footprints
    HIGH_HEELED_SHOE("👠"), //https://www.emojibase.com/emoji/1f460/highheeledshoe
    WOMAN_SANDAL("👡"), //https://www.emojibase.com/emoji/1f461/womanssandal
    WOMAN_BOOTS("👢"), //https://www.emojibase.com/emoji/1f462/womansboots
    MAN_SHOE("👞"), //https://www.emojibase.com/emoji/1f45e/mansshoe
    ATHLETIC_SHOE("👟"), //https://www.emojibase.com/emoji/1f45f/athleticshoe
    WOMAN_HAT("👒"), //https://www.emojibase.com/emoji/1f452/womanshat
    TOP_HAT("🎩"), //https://www.emojibase.com/emoji/1f3a9/tophat

    //Row#: 25
    GRADUATION_CAP("🎓"), //https://www.emojibase.com/emoji/1f393/graduationcap
    CROWN("👑"), //https://www.emojibase.com/emoji/1f451/crown
    HELMET_WITH_WHITE_CROSS("⛑"), //https://www.emojibase.com/emoji/26d1/helmetwithwhitecross
    SCHOOL_SATCHEL("🎒"), //https://www.emojibase.com/emoji/1f392/schoolsatchel
    POUCH("👝"), //https://www.emojibase.com/emoji/1f45d/pouch
    PURSE("👛"), //https://www.emojibase.com/emoji/1f45b/purse
    HANDBAG("👜"), //https://www.emojibase.com/emoji/1f45c/handbag
    BRIEFCASE("💼"), //https://www.emojibase.com/emoji/1f4bc/briefcase

    //Row#: 26
    EYE_GLASSES("👓"), //https://www.emojibase.com/emoji/1f453/eyeglasses
    DARK_SUN_GLASSES("🕶"), //https://www.emojibase.com/emoji/1f576/darksunglasses
    RING("💍"), //https://www.emojibase.com/emoji/1f48d/ring
    CLOSED_UMBRELLA("🌂"); //https://www.emojibase.com/emoji/1f302/closedumbrella

    override fun toString(): String {
        return symbol
    }
}