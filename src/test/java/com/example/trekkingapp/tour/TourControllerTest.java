package com.example.trekkingapp.tour;

import com.example.trekkingapp.common.ApiResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourControllerTest {

    @Test
    void getToursUsesPublishedToursServiceMethod() {
        TourService tourService = mock(TourService.class);
        TourController controller = new TourController(tourService);

        TourResponse publishedTour = new TourResponse(
                2L,
                10L,
                "Provider",
                20L,
                "Route",
                "Published Tour",
                "Published description",
                BigDecimal.valueOf(100),
                10,
                "MODERATE",
                "2 days",
                "Basecamp",
                "PUBLISHED",
                null,
                null,
                null,
                null
        );

        when(tourService.findPublishedTours()).thenReturn(List.of(publishedTour));

        ApiResponse<List<TourResponse>> response = controller.getTours();

        assertNotNull(response);
        assertNotNull(response.data());
        assertEquals(1, response.data().size());
        assertEquals("PUBLISHED", response.data().getFirst().status());
        verify(tourService).findPublishedTours();
    }
}
