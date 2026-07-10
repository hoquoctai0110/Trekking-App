package com.example.trekkingapp.tour;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.media.MediaStorageService;
import com.example.trekkingapp.media.UploadResult;
import com.example.trekkingapp.role.Role;
import com.example.trekkingapp.role.UserRole;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private TourImageRepository tourImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private MediaStorageService mediaStorageService;

    private TourService tourService;

    @BeforeEach
    void setUp() {
        tourService = new TourService(
                tourRepository,
                tourProviderRepository,
                routeRepository,
                tourImageRepository,
                userRepository,
                currentUserService,
                mediaStorageService
        );
    }

    @Test
    void findPublishedToursReturnsCoverImageUrlForPublicListing() {
        Tour publishedTour = createTour(2L, "Published Tour", "PUBLISHED");
        publishedTour.setCoverImageUrl("https://cdn.example.com/tours/2/cover.jpg");

        when(tourRepository.findPublishedToursWithRelations("PUBLISHED")).thenReturn(List.of(publishedTour));

        List<TourResponse> result = tourService.findPublishedTours();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Published Tour", result.getFirst().title());
        assertEquals("https://cdn.example.com/tours/2/cover.jpg", result.getFirst().coverImageUrl());
        assertNull(result.getFirst().images());
    }

    @Test
    void findByIdReturnsGalleryImages() {
        Tour tour = createTour(4L, "Tour Detail", "PUBLISHED");
        tour.setCoverImageUrl("https://cdn.example.com/tours/4/cover.jpg");
        TourImage coverImage = createTourImage(10L, tour, "https://cdn.example.com/tours/4/cover.jpg", "public-cover", 0, true);
        TourImage galleryImage = createTourImage(11L, tour, "https://cdn.example.com/tours/4/gallery.jpg", "public-gallery", 1, false);

        when(tourRepository.findByTourIdWithRelations(4L, "DELETED")).thenReturn(Optional.of(tour));
        when(tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(4L)).thenReturn(List.of(coverImage, galleryImage));

        TourResponse response = tourService.findById(4L);

        assertEquals("https://cdn.example.com/tours/4/cover.jpg", response.coverImageUrl());
        assertNotNull(response.images());
        assertEquals(2, response.images().size());
        assertEquals(10L, response.images().getFirst().imageId());
        assertEquals(0, response.images().getFirst().displayOrder());
        assertEquals(1, response.images().get(1).displayOrder());
    }

    @Test
    void uploadTourImagesSupportsMultipleFilesAndSetsFirstAsCover() {
        Tour tour = createTour(5L, "Upload Tour", "DRAFT");
        TourProvider provider = createProvider(10L, 100L);
        MockMultipartFile file1 = new MockMultipartFile("files", "cover.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "gallery.png", "image/png", new byte[]{2});

        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(createUser(100L, "TOUR_PROVIDER")));
        when(tourProviderRepository.findByUser_UserId(100L)).thenReturn(Optional.of(provider));
        when(tourRepository.findByTourIdWithRelations(5L, "DELETED")).thenReturn(Optional.of(tour));
        when(tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(5L)).thenReturn(List.of());
        when(mediaStorageService.uploadImage(file1, "tours/5"))
                .thenReturn(new UploadResult(null, "https://cdn.example.com/tours/5/cover.jpg", "public-cover", 100, 100, "jpg", 1000L));
        when(mediaStorageService.uploadImage(file2, "tours/5"))
                .thenReturn(new UploadResult(null, "https://cdn.example.com/tours/5/gallery.png", "public-gallery", 100, 100, "png", 900L));
        doAnswer(invocation -> {
            TourImage image = invocation.getArgument(0);
            if (image.getImageId() == null) {
                image.setImageId((long) (100 + image.getDisplayOrder()));
            }
            return image;
        }).when(tourImageRepository).save(any(TourImage.class));

        List<TourImageResponse> result = tourService.uploadTourImages(5L, List.of(file1, file2));

        assertEquals(2, result.size());
        assertEquals("https://cdn.example.com/tours/5/cover.jpg", result.getFirst().imageUrl());
        assertEquals(0, result.getFirst().displayOrder());
        assertEquals("https://cdn.example.com/tours/5/cover.jpg", tour.getCoverImageUrl());
        verify(mediaStorageService, times(2)).uploadImage(any(MockMultipartFile.class), anyString());
        verify(tourRepository).save(tour);
    }

    @Test
    void uploadTourImagesRejectsUserWhoDoesNotOwnTour() {
        Tour tour = createTour(6L, "Forbidden Tour", "DRAFT");
        MockMultipartFile file = new MockMultipartFile("files", "cover.jpg", "image/jpeg", new byte[]{1});

        when(currentUserService.getCurrentUserId()).thenReturn(200L);
        when(userRepository.findById(200L)).thenReturn(Optional.of(createUser(200L, "TOUR_PROVIDER")));
        when(tourProviderRepository.findByUser_UserId(200L)).thenReturn(Optional.of(createProvider(11L, 200L)));
        when(tourRepository.findByTourIdWithRelations(6L, "DELETED")).thenReturn(Optional.of(tour));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tourService.uploadTourImages(6L, List.of(file))
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(mediaStorageService, never()).uploadImage(any(), anyString());
    }

    @Test
    void setCoverImageMarksSelectedImageAsCover() {
        Tour tour = createTour(7L, "Cover Tour", "DRAFT");
        TourProvider provider = createProvider(10L, 100L);
        TourImage first = createTourImage(1L, tour, "https://cdn.example.com/1.jpg", "public-1", 0, true);
        TourImage second = createTourImage(2L, tour, "https://cdn.example.com/2.jpg", "public-2", 1, false);
        List<TourImage> images = new ArrayList<>(List.of(first, second));

        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(createUser(100L, "TOUR_PROVIDER")));
        when(tourProviderRepository.findByUser_UserId(100L)).thenReturn(Optional.of(provider));
        when(tourRepository.findByTourIdWithRelations(7L, "DELETED")).thenReturn(Optional.of(tour));
        when(tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(7L)).thenReturn(images);

        TourImageResponse response = tourService.setCoverImage(7L, 2L);

        assertEquals(2L, response.imageId());
        assertEquals("https://cdn.example.com/2.jpg", tour.getCoverImageUrl());
        assertEquals(Boolean.FALSE, first.getIsCover());
        assertEquals(Boolean.TRUE, second.getIsCover());
        verify(tourImageRepository).saveAll(images);
    }

    @Test
    void deleteTourImageRemovesCloudinaryAssetAndFallbackCover() {
        Tour tour = createTour(8L, "Delete Image Tour", "DRAFT");
        TourProvider provider = createProvider(10L, 100L);
        TourImage cover = createTourImage(31L, tour, "https://cdn.example.com/cover.jpg", "public-cover", 0, true);
        TourImage gallery = createTourImage(32L, tour, "https://cdn.example.com/gallery.jpg", "public-gallery", 1, false);

        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(createUser(100L, "TOUR_PROVIDER")));
        when(tourProviderRepository.findByUser_UserId(100L)).thenReturn(Optional.of(provider));
        when(tourRepository.findByTourIdWithRelations(8L, "DELETED")).thenReturn(Optional.of(tour));
        when(tourImageRepository.findByImageIdAndTour_TourId(31L, 8L)).thenReturn(Optional.of(cover));
        when(tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(8L)).thenReturn(List.of(gallery));

        String result = tourService.deleteTourImage(8L, 31L);

        assertEquals("Tour image deleted successfully", result);
        assertEquals("https://cdn.example.com/gallery.jpg", tour.getCoverImageUrl());
        verify(mediaStorageService).deleteImage("public-cover");
        verify(tourImageRepository).save(gallery);
    }

    @Test
    void uploadTourImagesCleansUpCloudinaryWhenDatabaseSaveFails() {
        Tour tour = createTour(9L, "Cleanup Tour", "DRAFT");
        TourProvider provider = createProvider(10L, 100L);
        MockMultipartFile file = new MockMultipartFile("files", "cover.jpg", "image/jpeg", new byte[]{1});
        UploadResult uploadResult = new UploadResult(null, "https://cdn.example.com/tours/9/cover.jpg", "public-9", 100, 100, "jpg", 1000L);

        when(currentUserService.getCurrentUserId()).thenReturn(100L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(createUser(100L, "TOUR_PROVIDER")));
        when(tourProviderRepository.findByUser_UserId(100L)).thenReturn(Optional.of(provider));
        when(tourRepository.findByTourIdWithRelations(9L, "DELETED")).thenReturn(Optional.of(tour));
        when(tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(9L)).thenReturn(List.of());
        when(mediaStorageService.uploadImage(file, "tours/9")).thenReturn(uploadResult);
        doThrow(new IllegalStateException("db failed")).when(tourImageRepository).save(any(TourImage.class));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tourService.uploadTourImages(9L, List.of(file))
        );

        assertEquals("db failed", exception.getMessage());
        verify(mediaStorageService).deleteImage("public-9");
    }

    private Tour createTour(Long tourId, String title, String status) {
        TourProvider provider = createProvider(10L, 100L);

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

    private TourImage createTourImage(Long imageId, Tour tour, String imageUrl, String publicId, int order, boolean isCover) {
        TourImage image = new TourImage();
        image.setImageId(imageId);
        image.setTour(tour);
        image.setImageUrl(imageUrl);
        image.setPublicId(publicId);
        image.setDisplayOrder(order);
        image.setIsCover(isCover);
        return image;
    }

    private TourProvider createProvider(Long providerId, Long userId) {
        TourProvider provider = new TourProvider();
        provider.setProviderId(providerId);
        provider.setCompanyName("Provider");
        provider.setUser(createUser(userId, "TOUR_PROVIDER"));
        return provider;
    }

    private User createUser(Long userId, String roleName) {
        User user = new User();
        user.setUserId(userId);
        user.setFullName("User " + userId);

        if (roleName != null) {
            Role role = new Role();
            role.setRoleName(roleName);

            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            user.setUserRoles(List.of(userRole));
        }

        return user;
    }
}
