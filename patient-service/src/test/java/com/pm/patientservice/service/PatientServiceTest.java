package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exeption.EmailAlreadyExistsException;
import com.pm.patientservice.exeption.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private BillingServiceGrpcClient billingServiceGrpcClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private PatientService patientService;

    @Test
    void getPatient_shouldReturnMappedPatients() {
        Patient first = patient(
                UUID.randomUUID(),
                "John Doe",
                "john@example.com",
                "Address 1",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2024, 1, 1)
        );
        Patient second = patient(
                UUID.randomUUID(),
                "Jane Doe",
                "jane@example.com",
                "Address 2",
                LocalDate.of(1992, 2, 2),
                LocalDate.of(2024, 2, 2)
        );
        when(patientRepository.findAll()).thenReturn(List.of(first, second));

        List<PatientResponseDTO> response = patientService.getPatient();

        assertEquals(2, response.size());
        assertEquals(first.getId().toString(), response.get(0).getId());
        assertEquals(second.getEmail(), response.get(1).getEmail());
        verify(patientRepository).findAll();
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @Test
    void createPatient_shouldThrowWhenEmailAlreadyExists() {
        PatientRequestDTO request = request(
                "John Doe",
                "john@example.com",
                "Address 1",
                "1990-01-01",
                "2024-01-01"
        );
        when(patientRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> patientService.createPatient(request));

        verify(patientRepository).existsByEmail(request.getEmail());
        verify(patientRepository, never()).save(any(Patient.class));
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @Test
    void createPatient_shouldSaveAndPublishWhenRequestIsValid() {
        UUID id = UUID.randomUUID();
        PatientRequestDTO request = request(
                "John Doe",
                "john@example.com",
                "Address 1",
                "1990-01-01",
                "2024-01-01"
        );

        Patient savedPatient = patient(
                id,
                request.getName(),
                request.getEmail(),
                request.getAddress(),
                LocalDate.parse(request.getDateOfBirth()),
                LocalDate.parse(request.getRegisteredDate())
        );

        when(patientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientResponseDTO response = patientService.createPatient(request);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient mapped = patientCaptor.getValue();
        assertEquals(request.getName(), mapped.getName());
        assertEquals(request.getEmail(), mapped.getEmail());
        assertEquals(LocalDate.parse(request.getDateOfBirth()), mapped.getDateOfBirth());
        assertEquals(LocalDate.parse(request.getRegisteredDate()), mapped.getRegisteredDate());

        verify(billingServiceGrpcClient).createBillingAccount(
                savedPatient.getId().toString(),
                savedPatient.getName(),
                savedPatient.getEmail()
        );
        verify(kafkaProducer).sendEvent(savedPatient);
        assertEquals(savedPatient.getId().toString(), response.getId());
    }

    @Test
    void updatePatient_shouldThrowWhenPatientDoesNotExist() {
        UUID id = UUID.randomUUID();
        PatientRequestDTO request = request(
                "John Doe",
                "john@example.com",
                "Address 1",
                "1990-01-01",
                "2024-01-01"
        );
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> patientService.updatePatient(id, request));

        verify(patientRepository).findById(id);
        verify(patientRepository, never()).existsByEmailAndIdNot(any(), any());
        verify(patientRepository, never()).save(any(Patient.class));
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @Test
    void updatePatient_shouldThrowWhenEmailBelongsToAnotherPatient() {
        UUID id = UUID.randomUUID();
        Patient existing = patient(
                id,
                "John Doe",
                "existing@example.com",
                "Address 1",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2024, 1, 1)
        );
        PatientRequestDTO request = request(
                "John Doe Updated",
                "duplicate@example.com",
                "New Address",
                "1991-02-02",
                "2024-01-01"
        );

        when(patientRepository.findById(id)).thenReturn(Optional.of(existing));
        when(patientRepository.existsByEmailAndIdNot(request.getEmail(), id)).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> patientService.updatePatient(id, request));

        verify(patientRepository).findById(id);
        verify(patientRepository).existsByEmailAndIdNot(request.getEmail(), id);
        verify(patientRepository, never()).save(any(Patient.class));
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @Test
    void updatePatient_shouldUpdateAndReturnDtoWhenRequestIsValid() {
        UUID id = UUID.randomUUID();
        Patient existing = patient(
                id,
                "John Doe",
                "john@example.com",
                "Address 1",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2024, 1, 1)
        );
        PatientRequestDTO request = request(
                "John Updated",
                "john.updated@example.com",
                "New Address",
                "1991-02-02",
                "2024-01-01"
        );

        when(patientRepository.findById(id)).thenReturn(Optional.of(existing));
        when(patientRepository.existsByEmailAndIdNot(request.getEmail(), id)).thenReturn(false);
        when(patientRepository.save(existing)).thenReturn(existing);

        PatientResponseDTO response = patientService.updatePatient(id, request);

        assertEquals(request.getName(), existing.getName());
        assertEquals(request.getEmail(), existing.getEmail());
        assertEquals(request.getAddress(), existing.getAddress());
        assertEquals(LocalDate.parse(request.getDateOfBirth()), existing.getDateOfBirth());
        assertEquals(id.toString(), response.getId());
        verify(patientRepository).save(existing);
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @Test
    void deletePatient_shouldThrowWhenPatientDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> patientService.deletePatient(id));

        verify(patientRepository).findById(id);
        verify(patientRepository, never()).deleteById(any(UUID.class));
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    @Test
    void deletePatient_shouldDeleteAndReturnDtoWhenPatientExists() {
        UUID id = UUID.randomUUID();
        Patient existing = patient(
                id,
                "John Doe",
                "john@example.com",
                "Address 1",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2024, 1, 1)
        );
        when(patientRepository.findById(id)).thenReturn(Optional.of(existing));

        PatientResponseDTO response = patientService.deletePatient(id);

        verify(patientRepository).deleteById(id);
        assertEquals(existing.getId().toString(), response.getId());
        assertEquals(existing.getEmail(), response.getEmail());
        verifyNoInteractions(billingServiceGrpcClient, kafkaProducer);
    }

    private static PatientRequestDTO request(
            String name,
            String email,
            String address,
            String dateOfBirth,
            String registeredDate
    ) {
        PatientRequestDTO request = new PatientRequestDTO();
        request.setName(name);
        request.setEmail(email);
        request.setAddress(address);
        request.setDateOfBirth(dateOfBirth);
        request.setRegisteredDate(registeredDate);
        return request;
    }

    private static Patient patient(
            UUID id,
            String name,
            String email,
            String address,
            LocalDate dateOfBirth,
            LocalDate registeredDate
    ) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName(name);
        patient.setEmail(email);
        patient.setAddress(address);
        patient.setDateOfBirth(dateOfBirth);
        patient.setRegisteredDate(registeredDate);
        return patient;
    }
}
