package electionguard.json

import electionguard.core.Base64.fromBase64
import electionguard.model.ElectionConstants
import kotlinx.serialization.Serializable

/* election_parameters.jcon
{
  "fixed_parameters": {
    "ElectionGuard_Design_Specification": {
      "Official": {
        "version": [
          2,
          0
        ],
        "release": "Release"
      }
    },
    "generation_parameters": {
      "q_bits_total": 256,
      "p_bits_total": 4096,
      "p_bits_msb_fixed_1": 256,
      "p_middle_bits_source": "ln_2",
      "p_bits_lsb_fixed_1": 256
    },
    "p": "base64://////////////////////////////////////////+xchf30c95q8njs5gD8vavQPNDJnKYti2KDRdbi6r6K+e4diBt66yYVZVS+0r6GxDtLq41wTghRCdXOykRabglPpbKFiJK6MUay9oRMXw4frnqm8OxNmA7JW+g7HZX90tyzoexnWVIyvXfpr04MDJIZV+hhy8g46LaLZfFDz/Vxgf0yhH7W/uQYQ0w+I/lUaLuVp1t/B76FX0uI94UALOWF0YHdduJkOXJQ+cytHnNM8zMTmWTk1FMTBoeC9HWCV0lZpszNWSjN2qcf4peFI6CB022AVz+eqMMSApUFzUNLJVdUXmx4XudrjE822xgbLEHj3NdGy2zG19QtRhQZMGLTRYts7NlhT11mKGVGuJz7lVwtsaPlpg0ltTm0zCviJtEoCVUcxzcjqFyk9Eiik75jW9Rd/vPB1UmilwflTi5gmGv/URrHKPPXpIiuIxm08VCIYPtyZQhCQu7Fvrz2UnyNuArIM7ohrkFwSjVPQvS+WITYxlq9QMCAGDkmQg5GgxXM5uivrp9BSrFthzE6SB87y8M4tc3OVjXYiZYkERXRPtfLaS3UQBYktNWiQ3v6crZudS3E+BhYqLY/dDfL9YI//////////////////////////////////////////8=",
    "q": "base64://///////////////////////////////////////0M=",
    "r": "base64:AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8sXIX99HPeavJ47OYA/L2r0DzQyZymLYtig0XW4urhXro9CgWVBiAbGKw6jY1Wjpz4MdBmFv2oOMTAXm/LwtD4zrYYpI4YbjJ92jEFpUZYAutBgk/lksn4C2GgxIxqRYN5I9NpT2KteaeOGtpS+wa5yLUdXkknVQkdnxcM7kVHgfFwR0QasRG0zC0fbWdNS5HpTFX3gRGGQD2/jYNuJffUxbYfJSucdrQvoS2R8S8+BjCOi1Ou1PHAqXIBi0Z9em1AzqU9/9zL1QSlxKGnZe4yWxBKSGp2GeXcPSZoEHCl8/3nUyRSetsr2e56j3FY9ll86rRN3/yLenD5iBo3Q7WFRw3tPdGNMK9CdqRL9WZ9DM6jSzABWJ9yje61D5ko5YxGcC/40gQoh7nz8Qh1TOYy8epWzv1heWgS3kOL+H+m8Jk/agQn2RUoIL177LzfqI3qinfMg1uqGDEGpBUzNJIdsYlP2Z7+wE5tVMf8wGJlhIC/SsNVadScsf9czQ/eJm8oLNqTEcKZKAJJEyE53zrySQX1bsTvxgWfYAz62xN14ef1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg==",
    "g": "base64:NgNv7SFPO1DcVm06MS/kEx/uHCvObQLqObR3rAX3+IXzjP53p+Raz0ApEUxNepv+BYvy+ZXSR5092mGP/ZENPEI2qyz914OlAW90Zc9Zu/RdJKIvEw8tBP6TstWLucHR0n/JoX0q9Jp3nz/73KIpAMFCAu5smWFgNL41y83T57t5lq3+U0tjzKQeIf9dx3jrsbhsU7++mZh9euoHViN/tAkiE5+Qpi8qqNmtNN/3meM8hXpkaNABrPO2gduH3EJCdV4qxaUCfbgZhPAzxNF4Nx8nPbtPzqHmKMI+UnWbx3ZXKANc6ia0TEmmVmaImCCkXDPdN+pKHQDLYjBc1UG+HoqSaFoHASsaIKdGw1kaLbOBUADSqsz+Q9xJ6CjB7XOHRmr9jkvxk1WTsqRC7sJxxQrTn3M3l6HqEYAqJVeRZTRmKmt+mp5EmiTIz/gJ55pNgG62gRGTMObFeYXjmyALSJNjn9/epJ92rRrNmX66E2V1QeeexXQ35QTtqd0BEGFRbGQ/sw1tWK/M0otz/top7BKwGl64Y5mlk6nV9FDeOcuSlixexpJTSNtU0Sj9mcFLRX+IPsIBEqdaagWB09gKO07wnshvlVL/2hZT8TOqJTSYOm8xsO5Gl5Naax6i91uF5+uhUbpIYJTWhyKwVGM/7FHKPymzHnfjF7F4trnYrg8="
  },
  "varying_parameters": {
    "n": 5,
    "k": 3,
    "date": "11/29/2023",
    "info": "info",
    "ballot_chaining": "Prohibited"
  }
}

 */

@Serializable
data class ElectionParametersJsonR(
    val fixed_parameters : FixedParametersJsonR,
    val varying_parameters : VaryingParameters,
)

fun ElectionParametersJsonR.import() = ElectionParameters(
    this.fixed_parameters.import(),
    this.varying_parameters,
)

@Serializable
data class FixedParametersJsonR(
    val p: String,
    val q: String,
    val r: String,
    val g: String,
)

fun FixedParametersJsonR.import() : ElectionConstants {
    return ElectionConstants( "FixedParametersJsonR",
        this.p.substring(7).fromBase64()!!,  // strip off "base64:"
        this.q.substring(7).fromBase64()!!,
        this.r.substring(7).fromBase64()!!,
        this.g.substring(7).fromBase64()!!,
    )
}

@Serializable
data class VaryingParameters(
    val n: Int,
    val k: Int,
    val date: String,
    val info: String,
    val ballot_chaining: String,
)

data class ElectionParameters(
    val electionConstants : ElectionConstants,
    val varyingParameters : VaryingParameters,
)
