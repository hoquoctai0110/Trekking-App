package com.example.trekkingapp.admin;

import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.service.AdminUserService;
import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.common.ForbiddenOperationException;
import com.example.trekkingapp.role.RoleRepository;
import com.example.trekkingapp.role.UserRoleRepository;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    @Test
    void banUserRejectsCurrentAdmin() {
        UserRepository userRepository = mock(UserRepository.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        TourRepository tourRepository = mock(TourRepository.class);
        TourProviderRepository tourProviderRepository = mock(TourProviderRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);

        AdminUserService service = new AdminUserService(
                userRepository,
                bookingRepository,
                tourRepository,
                tourProviderRepository,
                roleRepository,
                userRoleRepository,
                currentUserService,
                adminAuditService
        );

        User admin = new User();
        admin.setUserId(99L);
        admin.setStatus("ACTIVE");

        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> service.banUser(99L, new AdminActionReasonRequest("policy violation"))
        );

        assertEquals("Admin cannot ban the current account", exception.getMessage());
        verify(userRepository, never()).save(admin);
    }
}
