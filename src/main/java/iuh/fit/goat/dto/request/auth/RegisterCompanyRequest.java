package iuh.fit.goat.dto.request.auth;

import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.CompanySize;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCompanyRequest {
    @NotBlank(message = "Tên hiển thị không được để trống")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, chữ hoa, chữ thường, số và ký tự đặc biệt"
    )
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 50, message = "Mô tả công ty phải có ít nhất 50 ký tự")
    private String description;

    @NotBlank(message = "Logo không được để trống")
    private String logo;

    @NotBlank(message = "Cover photo không được để trống")
    private String coverPhoto;

    @Pattern(
            regexp = "^(https?:\\/\\/)?([\\w-]+\\.)+[\\w-]+(\\/\\S*)?$",
            message = "Website không hợp lệ"
    )
    private String website;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp="^\\d{10}$",
            message="Số điện thoại không hợp lệ"
    )
    private String phone;

    @NotNull(message = "Kích thước công ty không được để trống")
    private CompanySize size;

    @NotBlank(message = "Quốc gia không được để trống")
    private String country;

    @NotBlank(message = "Lĩnh vực không được để trống")
    private String industry;

    @NotBlank(message = "Ngày làm việc không được để trống")
    private String workingDays;

    @NotBlank(message = "Chính sách làm thêm không được để trống")
    private String overtimePolicy;

    @NotEmpty(message = "Phải có ít nhất một địa chỉ")
    private List<Address> addresses;

    @AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp")
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
}
