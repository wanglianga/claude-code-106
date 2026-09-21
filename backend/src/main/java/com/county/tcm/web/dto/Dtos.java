package com.county.tcm.web.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/** 全部请求 DTO 集中定义 */
public final class Dtos {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record HerbItemDto(
            @NotBlank String name,
            @NotNull @DecimalMin(value = "0.1", message = "剂量必须大于0") BigDecimal dosePerPacket,
            String specialMethod,
            String note) {}

    public record RxCreateRequest(
            @NotBlank String patientName,
            String patientPhone,
            String clinicName,
            Long doctorId,
            String doctorName,
            @NotNull @Min(value = 1, message = "剂数至少1") Integer doses,
            String specialDecoctionNote,
            boolean addSugar,
            @NotBlank String pickupMethod,
            String address,
            String contactName,
            String contactPhone,
            String contraindications,
            @NotBlank String settlementType,
            boolean nightUrgent,
            String batchNo,
            @NotEmpty(message = "至少需要一味药") List<HerbItemDto> herbs) {}

    public record BatchCreateRequest(@NotEmpty @Valid List<RxCreateRequest> prescriptions) {}

    public record ReviewSubmitRequest(
            @NotBlank String conclusion,
            boolean applySuggestions,
            String insuranceNote,
            String remark) {}

    public record DispenseRequest(String note) {}

    public record DecoctScheduleRequest(
            @NotBlank String potNo,
            @NotNull @Min(1) Integer soakMinutes,
            @NotNull @Min(1) Integer boilTimes,
            @NotNull @Min(1) Integer bagCount,
            String deliveryWave) {}

    public record ScanRequest(@NotBlank String bagCode) {}

    public record DecoctEndRequest(
            String abnormalSmell,
            Integer missingBags,
            Integer damagedBags,
            @NotBlank String packer,
            @NotBlank String reviewer,
            String remark) {}

    public record DeliveryAssignRequest(Long courierId, String wave, Integer promisedMinutes) {}

    public record SignRequest(String signedBy, String address, String proxyName) {}

    public record AddressChangeRequest(@NotBlank String newAddress) {}

    public record FollowUpRequest(
            @NotBlank String result,
            String symptoms,
            String advice,
            @Min(1) @Max(5) Integer satisfactionScore) {}

    public record IssueCreateRequest(@NotBlank String type, String description) {}

    public record IssueResolveRequest(String resolution, BigDecimal compensation) {}

    public record InvoiceRequest(String title) {}

    public record ReminderStateRequest(@NotBlank String state, String note) {}
}
