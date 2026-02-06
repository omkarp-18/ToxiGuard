package com.example.toxiguard.util

class SmartReplyGenerator {

    // ----------------------------------------------------------
    // 100+ COMMON HARMFUL / TOXIC WORDS (AGGRESSION + INSULTS)
    // ----------------------------------------------------------
    private val toxicKeywords = listOf(
        "idiot","stupid","useless","dumb","fool","moron","shit","fuck","fucking","bastard","bitch",
        "loser","trash","garbage","kill","hate","disgusting","ugly","nonsense","worthless","clown",
        "shut up","get lost","pig","dog","snake","liar","cheater","fraud","fake","pathetic","crybaby",
        "psycho","hell","toxic","annoying","irritating","nobody","idiotic","stinks","sucks","jerk",
        "dumbass","asshole","prick","weirdo","creep","witch","dirt","scum","worm","freak","stinky",
        "nasty","brainless","backstabber","imbecile","shameful","ridiculous","weak","coward",
        "disrespectful","retarded","mad","crazy","mental","broken","obsessed","delusional",
        "shut your mouth","waste","failure","kill yourself","burn","die","hang","curse","filthy",
        "bite me","cry more","pathetic loser","narcissist","idiot brain","I hate you","go die",
        "I’ll hit you","I’ll destroy you","I’ll ruin you","you’re nothing","you’re trash","disgust me"
    )

    val abuse = listOf(
        "idiot","stupid","dumb","moron","fool","useless","worthless","clown","trash",
        "loser","pathetic","jerk","lame","crybaby","weak","coward","pig","snake",
        "fake","fraud","two-faced","liar","cheater","dumbass","airhead"
    )
    val profanity = listOf(
        "hell","damn","bloody","bastard","bitch","asshole","piss off",
        "fuck","fucked","fucker","shit","bullshit","crap",
        "dick","dickhead","prick","slut","whore"
    )
    val threats = listOf(
        "i'll hit you","i'll slap you","i'll hurt you","beat you","kill you",
        "destroy you","ruin you","watch your back","you better run",
        "i'll find you","you'll regret this","break your face"
    )
    val gaslight = listOf(
        "you're imagining things","you're overreacting","nobody likes you",
        "you're too sensitive","you always ruin everything",
        "what's wrong with you","grow up","stop acting like a child"
    )
    val appearance = listOf(
        "ugly","fat","skinny","shorty","creep","weirdo","nerd","geek",
        "disgusting","hideous"
    )


    // ----------------------------------------------------------
    // MULTIPLE SMART REPLIES (CALM, NEUTRAL, LOGICAL/BOUNDARY)
    // ----------------------------------------------------------
    private val calmReplies = listOf(
        "Let's stay calm.",
        "I understand.",
        "Okay, noted.",
        "Let's take a moment. I’d prefer to talk about this calmly.",
        "I'm choosing to stay calm right now.",
        "Let's slow down a bit."
    )

    private val neutralReplies = listOf(
        "Hmm, alright.",
        "Got it.",
        "Thanks for sharing.",
        "I understand. Let's discuss this without getting upset.",
        "Okay, I'm listening.",
        "I see your point."
    )

    private val boundaryReplies = listOf(
        "Please be respectful.",
        "That's not okay.",
        "Let's not go there.",
        "Let's stay objective. Can you explain what you mean clearly?",
        "I want to keep this conversation respectful.",
        "Let's talk without insults."
    )

    // ----------------------------------------------------------
    // MAIN FUNCTION — CLASSIFY & RETURN REPLIES
    // ----------------------------------------------------------
    fun getRepliesFor(text: String): List<String> {

        val lower = text.lowercase()

        val matchedKeywords = toxicKeywords.filter { lower.contains(it) }

        if (matchedKeywords.isEmpty()) {
            return emptyList()
        }

        // HIGH AGGRESSION / VIOLENCE
        if (matchedKeywords.any { it in listOf("kill","die","burn","hang","destroy","ruin","kill yourself") }) {
            return boundaryReplies.shuffled().take(3) // strict replies
        }

        // MEDIUM INSULTS
        if (matchedKeywords.size >= 3) {
            return neutralReplies.shuffled().take(3)
        }

        // LOW / MILD INSULTS
        return calmReplies.shuffled().take(3)
    }
}
