package com.example.trekkingapp.tour;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.media.MediaStorageService;
import com.example.trekkingapp.media.UploadResult;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class TourService {

    private static final Logger log = LoggerFactory.getLogger(TourService.class);

    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String ROLE_ADMIN = "ADMIN";

    private final TourRepository tourRepository;
    private final TourProviderRepository tourProviderRepository;
    private final RouteRepository routeRepository;
    private final TourImageRepository tourImageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final MediaStorageService mediaStorageService;

    public TourService(
            TourRepository tourRepository,
            TourProviderRepository tourProviderRepository,
            RouteRepository routeRepository,
            TourImageRepository tourImageRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            MediaStorageService mediaStorageService
    ) {
        this.tourRepository = tourRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.routeRepository = routeRepository;
        this.tourImageRepository = tourImageRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.mediaStorageService = mediaStorageService;
    }

    @Transactional
    public TourResponse createMyTour(TourRequest request) {
        TourProvider provider = findCurrentProvider();
        Route route = findActiveRoute(request.routeId());

        Tour tour = new Tour();
        tour.setProvider(provider);
        tour.setRoute(route);
        tour.setStatus(STATUS_DRAFT);
        applyRequest(tour, request);

        return toResponse(tourRepository.save(tour), false);
    }

    @Transactional(readOnly = true)
    public List<TourResponse> findMyTours() {
        TourProvider provider = findCurrentProvider();
        return tourRepository.findByProviderWithRelations(provider.getProviderId(), STATUS_DELETED)
                .stream()
                .map(tour -> toResponse(tour, false))
                .toList();
    }

    @Transactional
    public TourResponse updateMyTour(Long tourId, TourRequest request) {
        TourProvider provider = findCurrentProvider();
        Tour tour = findActiveTour(tourId);

        validateOwnership(tour, provider);
        tour.setRoute(findActiveRoute(request.routeId()));
        applyRequest(tour, request);

        return toResponse(tourRepository.save(tour), false);
    }

    @Transactional
    public String deleteMyTour(Long tourId) {
        TourProvider provider = findCurrentProvider();
        Tour tour = findActiveTour(tourId);

        validateOwnership(tour, provider);
        cleanupTourImages(tour);
        tour.setStatus(STATUS_DELETED);
        tourRepository.save(tour);

        return "Tour deleted successfully";
    }

    @Transactional(readOnly = true)
    public List<TourResponse> findPublishedTours() {
        List<Tour> publishedTours = tourRepository.findPublishedToursWithRelations(STATUS_PUBLISHED);
        log.info("public_tours_loaded count={}", publishedTours.size());
        return publishedTours
                .stream()
                .map(tour -> toResponse(tour, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public TourResponse findById(Long tourId) {
        return toResponse(findActiveTour(tourId), true);
    }

    @Transactional(readOnly = true)
    public List<TourResponse> findByProviderId(Long providerId) {
        return tourRepository.findByProviderWithRelations(providerId, STATUS_DELETED)
                .stream()
                .map(tour -> toResponse(tour, false))
                .toList();
    }

    @Transactional
    public List<TourImageResponse> uploadTourImages(Long tourId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image file is required");
        }

        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        List<UploadResult> uploadedFiles = new ArrayList<>();
        try {
            List<TourImage> existingImages = tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(tourId);
            int nextDisplayOrder = existingImages.size();
            boolean hasCover = existingImages.stream().anyMatch(image -> Boolean.TRUE.equals(image.getIsCover()));
            List<TourImageResponse> responses = new ArrayList<>();

            for (MultipartFile file : files) {
                UploadResult uploadResult = mediaStorageService.uploadImage(file, "tours/" + tourId);
                uploadedFiles.add(uploadResult);

                TourImage tourImage = new TourImage();
                tourImage.setTour(tour);
                tourImage.setImageUrl(resolveImageUrl(uploadResult));
                tourImage.setPublicId(uploadResult.publicId());
                tourImage.setDisplayOrder(nextDisplayOrder++);
                boolean isCover = !hasCover;
                tourImage.setIsCover(isCover);

                TourImage savedImage = tourImageRepository.save(tourImage);
                if (isCover) {
                    tour.setCoverImageUrl(savedImage.getImageUrl());
                    tour.setCoverImagePublicId(savedImage.getPublicId());
                    hasCover = true;
                }
                responses.add(toImageResponse(savedImage));
            }

            tourRepository.save(tour);
            return responses;
        } catch (RuntimeException exception) {
            cleanupUploadedResults(uploadedFiles);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<TourImageResponse> getTourImages(Long tourId) {
        findActiveTour(tourId);
        return tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(tourId)
                .stream()
                .map(this::toImageResponse)
                .toList();
    }

    @Transactional
    public TourImageResponse setCoverImage(Long tourId, Long imageId) {
        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        List<TourImage> images = tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(tourId);
        TourImage selectedImage = images.stream()
                .filter(image -> image.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour image not found"));

        for (TourImage image : images) {
            image.setIsCover(image.getImageId().equals(selectedImage.getImageId()));
        }
        tour.setCoverImageUrl(selectedImage.getImageUrl());
        tour.setCoverImagePublicId(selectedImage.getPublicId());
        tourRepository.save(tour);
        tourImageRepository.saveAll(images);

        return toImageResponse(selectedImage);
    }

    @Transactional
    public String deleteTourImage(Long tourId, Long imageId) {
        Tour tour = findActiveTour(tourId);
        validateTourAccess(tour);

        TourImage image = tourImageRepository.findByImageIdAndTour_TourId(imageId, tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour image not found"));

        tourImageRepository.delete(image);
        tourImageRepository.flush();
        mediaStorageService.deleteImage(image.getPublicId());

        List<TourImage> remainingImages = tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(tourId);
        if (remainingImages.isEmpty()) {
            tour.setCoverImageUrl(null);
            tour.setCoverImagePublicId(null);
        } else if (Boolean.TRUE.equals(image.getIsCover())) {
            TourImage nextCover = remainingImages.getFirst();
            nextCover.setIsCover(true);
            tour.setCoverImageUrl(nextCover.getImageUrl());
            tour.setCoverImagePublicId(nextCover.getPublicId());
            tourImageRepository.save(nextCover);
        }

        if (!remainingImages.isEmpty() && Boolean.FALSE.equals(remainingImages.getFirst().getIsCover())
                && remainingImages.stream().noneMatch(candidate -> Boolean.TRUE.equals(candidate.getIsCover()))) {
            TourImage nextCover = remainingImages.getFirst();
            nextCover.setIsCover(true);
            tour.setCoverImageUrl(nextCover.getImageUrl());
            tour.setCoverImagePublicId(nextCover.getPublicId());
            tourImageRepository.save(nextCover);
        }

        tourRepository.save(tour);
        return "Tour image deleted successfully";
    }

    private TourProvider findCurrentProvider() {
        Long currentUserId = currentUserService.getCurrentUserId();
        TourProvider provider = tourProviderRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found"));

        if (STATUS_DELETED.equals(provider.getStatus())) {
            throw new IllegalArgumentException("Provider profile is deleted");
        }

        return provider;
    }

    private User findCurrentUser() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private Route findActiveRoute(Long routeId) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
    }

    private Tour findActiveTour(Long tourId) {
        return tourRepository.findByTourIdWithRelations(tourId, STATUS_DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
    }

    private void validateOwnership(Tour tour, TourProvider provider) {
        if (!tour.getProvider().getProviderId().equals(provider.getProviderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this tour");
        }
    }

    private void validateTourAccess(Tour tour) {
        User currentUser = findCurrentUser();
        if (hasRole(currentUser, ROLE_ADMIN)) {
            return;
        }

        TourProvider provider = tourProviderRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tour provider profile not found"));

        validateOwnership(tour, provider);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getUserRoles()
                .stream()
                .anyMatch(userRole -> roleName.equals(userRole.getRole().getRoleName()));
    }

    private void applyRequest(Tour tour, TourRequest request) {
        tour.setTitle(request.title());
        tour.setDescription(request.description());
        tour.setPrice(request.price());
        tour.setMaxParticipants(request.maxParticipants());
        tour.setDifficulty(request.difficulty());
        tour.setDuration(request.duration());
        tour.setMeetingPoint(request.meetingPoint());
        tour.setStartDate(request.startDate());
        tour.setEndDate(request.endDate());
    }

    private TourResponse toResponse(Tour tour, boolean includeImages) {
        TourProvider provider = tour.getProvider();
        Route route = tour.getRoute();
        List<TourImageResponse> images = includeImages
                ? tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(tour.getTourId())
                .stream()
                .map(this::toImageResponse)
                .toList()
                : null;
        return new TourResponse(
                tour.getTourId(),
                provider.getProviderId(),
                provider.getCompanyName(),
                route.getRouteId(),
                route.getRouteName(),
                tour.getTitle(),
                tour.getDescription(),
                tour.getPrice(),
                tour.getMaxParticipants(),
                tour.getDifficulty(),
                tour.getDuration(),
                tour.getMeetingPoint(),
                tour.getCoverImageUrl(),
                images,
                tour.getStatus(),
                tour.getStartDate(),
                tour.getEndDate(),
                tour.getCreatedAt(),
                tour.getUpdatedAt()
        );
    }

    private TourImageResponse toImageResponse(TourImage image) {
        return new TourImageResponse(
                image.getImageId(),
                image.getImageUrl(),
                image.getDisplayOrder(),
                Boolean.TRUE.equals(image.getIsCover()),
                image.getCreatedAt()
        );
    }

    private String resolveImageUrl(UploadResult uploadResult) {
        return uploadResult.secureUrl() == null || uploadResult.secureUrl().isBlank()
                ? uploadResult.url()
                : uploadResult.secureUrl();
    }

    private void cleanupUploadedResults(List<UploadResult> uploadedFiles) {
        for (UploadResult uploadedFile : uploadedFiles) {
            if (uploadedFile == null || uploadedFile.publicId() == null || uploadedFile.publicId().isBlank()) {
                continue;
            }
            try {
                mediaStorageService.deleteImage(uploadedFile.publicId());
            } catch (RuntimeException cleanupException) {
                log.warn("tour_image_cleanup_failed publicId={} message={}",
                        uploadedFile.publicId(),
                        cleanupException.getMessage());
            }
        }
    }

    private void cleanupTourImages(Tour tour) {
        List<TourImage> images = tourImageRepository.findByTour_TourIdOrderByDisplayOrderAscImageIdAsc(tour.getTourId());
        List<String> publicIds = images.stream()
                .map(TourImage::getPublicId)
                .toList();

        tourImageRepository.deleteAll(images);
        tourImageRepository.flush();
        for (String publicId : publicIds) {
            mediaStorageService.deleteImage(publicId);
        }

        tour.setCoverImageUrl(null);
        tour.setCoverImagePublicId(null);
    }
}
