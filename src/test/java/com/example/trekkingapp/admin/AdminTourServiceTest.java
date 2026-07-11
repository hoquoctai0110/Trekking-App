package com.example.trekkingapp.admin;

import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.service.AdminTourService;
import com.example.trekkingapp.common.InvalidStateTransitionException;
import com.example.trekkingapp.review.ReviewRepository;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminTourServiceTest {

    @Test
    void publishRequiresApprovedStatus() {
        TourRepository tourRepository = mock(TourRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        AdminTourService service = new AdminTourService(tourRepository, reviewRepository, adminAuditService);

        Tour tour = createTour("DRAFT");
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour));

        InvalidStateTransitionException exception = assertThrows(
                InvalidStateTransitionException.class,
                () -> service.publish(1L)
        );

        assertEquals("Only approved tour can be published", exception.getMessage());
    }

    @Test
    void approveDraftTourChangesStatus() {
        TourRepository tourRepository = mock(TourRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        AdminTourService service = new AdminTourService(tourRepository, reviewRepository, adminAuditService);

        Tour tour = createTour("DRAFT");
        when(tourRepository.findById(1L)).thenReturn(Optional.of(tour));
        when(reviewRepository.findAverageRatingByTourId(1L)).thenReturn(Optional.of(4.5));
        when(reviewRepository.countByTour_TourIdAndDeletedAtIsNull(1L)).thenReturn(2L);

        assertEquals("APPROVED", service.approve(1L).status());
    }

    private Tour createTour(String status) {
        TourProvider provider = new TourProvider();
        provider.setCompanyName("Provider");

        Route route = new Route();
        route.setRouteName("Route");

        Tour tour = new Tour();
        tour.setTourId(1L);
        tour.setTitle("Tour");
        tour.setProvider(provider);
        tour.setRoute(route);
        tour.setStatus(status);
        tour.setPrice(BigDecimal.valueOf(100));
        tour.setMaxParticipants(10);
        return tour;
    }
}
