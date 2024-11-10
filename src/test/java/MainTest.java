import static org.junit.jupiter.api.Assertions.assertNull;
import static com.moloco.mcm.Main.main;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MainTest {

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testMain() {
        // Given

        // When
        main(new String[] {""});

        // Then
        assertNull(null);
    }
}