package ir.bumo.app

import ir.bumo.app.ui.components.imageUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageUrlTest {
    @Test fun relativePathUsesConfiguredServer() {
        assertEquals("http://192.168.1.10:8080/media/pins/1/474.jpg", imageUrl("http://192.168.1.10:8080", "/media/pins/1/474.jpg"))
    }

    @Test fun absoluteUrlIsKeptUntouched() {
        assertEquals("https://example.test/a.jpg", imageUrl("http://local", "https://example.test/a.jpg"))
    }
}
