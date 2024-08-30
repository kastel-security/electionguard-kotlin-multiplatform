import electionguard.core.BigInteger
import electionguard.core.toByteArray
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BigIntegerTest {
    companion object {
        const val B1 = "1EBC1E9BCC5FB0D9AC8E88F3914483403929656924518C36C596AB14A1BE8A9E"
        const val B1Neg = "-1EBC1E9BCC5FB0D9AC8E88F3914483403929656924518C36C596AB14A1BE8A9E"
        const val B2 = "7BE7222D305B814AC0FDA6DE3E4A352D66BC41CF4BEFB166436B63A66D82418D"
        const val B3 = "1a17149f0a34d268c22aca4c28a49d4891"
        const val BYTE_ARRAY = "[30,-68,30,-101,-52,95,-80,-39,-84,-114,-120,-13,-111,68,-125,64,57,41,101,105,36,81," +
                "-116,54,-59,-106,-85,20,-95,-66,-118,-98]"
        const val ADD = "69944621883753133895519505224147663926793107728451959395303521150277685136427"
        const val SUBTRACT = "-42141093966990058992511171806174634693717087046155987100631017737989627557615"
        const val MULTIPLY = "77909458243162796239018976814672018445696345616197393380098603769658961843995500309372" +
                "8448959978506233797122569863134263768882025140524339947862864459526"
        const val DIVIDE = "4"
        const val SHIFT_LEFT = "111214111667052299612033333671892116932304082729183889178690013649152230315248"
        const val SHIFT_RIGHT = "1737720494797692181438020838623314327067251292643498268417031463268003598675"
        const val AND = "12049911027958436085093477913806558526524743410516092433235740302630413533324"
        const val OR = "57894710855794697810426027310341105400268364317935866962067780847647271603103"
        const val MOD = "8546612639237662951821749923796763019187"
        const val MOD_INVERSE = "5105215397262132645229573080095298271248"
        const val POW = "1741232755121145635735186772276482304972"
    }

    @Test
    fun testBasicOperations() {
        val b1 = BigInteger(B1, 16)
        val b2 = BigInteger(B2, 16)
        val b3 = BigInteger(B3, 16)
        val b1Neg = BigInteger(B1Neg, 16)
        assertEquals(b1Neg, b1 * BigInteger("-1"))
        assertEquals(BYTE_ARRAY, Json.encodeToString(b1.toByteArray()))
        assertEquals(ADD, (b1 + b2).toString())
        assertEquals(SUBTRACT, (b1 - b2).toString())
        assertEquals(MULTIPLY, (b1 * b2).toString())
        assertEquals(DIVIDE, (b2 / b1).toString())
        assertEquals(SHIFT_LEFT, (b1 shl 3).toString())
        assertEquals(SHIFT_RIGHT, (b1 shr 3).toString())
        assertEquals(AND, b1.and(b2).toString())
        assertEquals(OR, b1.or(b2).toString())
        assertEquals(MOD, b1.mod(b3).toString())
        assertEquals(MOD_INVERSE, b1.modInverse(b3).toString())
        assertEquals(POW, b1.modPow(b2, b3).toString())
        assertEquals(b1, BigInteger(1, b1.toByteArray()))
        assertEquals(b1, BigInteger(b1.toByteArray()))
        assertEquals(b1, BigInteger(b1.toString(), 10))
        assertEquals(b1, BigInteger(b1.toString()))
        assertEquals(b1Neg, BigInteger(-1, b1.toByteArray()))
    }
}