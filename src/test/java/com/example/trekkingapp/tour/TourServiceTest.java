package com.example.trekkingapp.tour;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourProviderRepository tourProviderRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TourService tourService;

    @Test
    void findPublishedToursReturnsOnlyPublishedToursForPublicListing() {
        Tour draftTour = createTour(1L, "Draft Tour", "DRAFT");
        Tour publishedTour = createTour(2L, "Published Tour", "PUBLISHED");
        Tour cancelledTour = createTour(3L, "Cancelled Tour", "CANCELLED");

        when(tourRepository.findByStatus("PUBLISHED")).thenReturn(List.of(publishedTour));

        List<TourResponse> result = tourService.findPublishedTours();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Published Tour", result.getFirst().title());
        assertEquals("PUBLISHED", result.getFirst().status());

        verify(tourRepository).findByStatus("PUBLISHED");
        verify(tourRepository, never()).findByStatusNot(anyString());

        assertEquals("DRAFT", draftTour.getStatus());
        assertEquals("CANCELLED", cancelledTour.getStatus());
    }

    private Tour createTour(Long tourId, String title, String status) {
        TourProvider provider = new TourProvider();
        provider.setProviderId(10L);
        provider.setCompanyName("Provider");

        Route route = new Route();
        route.setRouteId(20L);
        route.setRouteName("Route");

        Tour tour = new Tour();
        tour.setTourId(tourId);
        tour.setProvider(provider);
        tour.setRoute(route);
        tour.setTitle(title);
        tour.setDescription(title + " description");
        tour.setPrice(BigDecimal.valueOf(100));
        tour.setMaxParticipants(10);
        tour.setDifficulty("MODERATE");
        tour.setDuration("2 days");
        tour.setMeetingPoint("Basecamp");
        tour.setStatus(status);
        return tour;
    }
}
