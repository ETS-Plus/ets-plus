package app.etsprivate.mail

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarInitialsTest {
    @Test fun twoWords() = assertEquals("JT", avatarInitials("Jean Tremblay"))
    @Test fun middleNameSkipped() = assertEquals("MD", avatarInitials("Marie Claude Dubois"))
    @Test fun lastFirstWithComma() = assertEquals("TJ", avatarInitials("Tremblay, Jean"))
    @Test fun addressParts() = assertEquals("PN", avatarInitials("prenom.nom.1@ens.etsmtl.ca"))
    @Test fun oneWord() = assertEquals("S", avatarInitials("Signets"))
    @Test fun quoted() = assertEquals("ÉS", avatarInitials("\"École Services\""))
    @Test fun nothing() = assertEquals("?", avatarInitials("  "))
}
