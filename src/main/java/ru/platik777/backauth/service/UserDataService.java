package ru.platik777.backauth.service;

import ru.platik777.backauth.dto.CompanyDto;
import ru.platik777.backauth.dto.StudentDto;
import ru.platik777.backauth.dto.UserDto;
import ru.platik777.backauth.dto.response.ApiResponseDto;
import ru.platik777.backauth.entity.Company;
import ru.platik777.backauth.entity.Student;
import ru.platik777.backauth.entity.User;
import ru.platik777.backauth.mapper.UserMapper;
import ru.platik777.backauth.repository.CompanyRepository;
import ru.platik777.backauth.repository.StudentRepository;
import ru.platik777.backauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с данными пользователей (компании, студенты)
 * Реализует логику из Go версии для получения данных пользователей
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final UserMapper userMapper;

    /**
     * Получение компаний пользователя (аналог GetCompanies из Go)
     */
    public ApiResponseDto<List<CompanyDto>> getCompaniesOfUser(Integer userId) {
        log.debug("Getting companies for userId: {}", userId);

        // Поиск компаний где пользователь является владельцем
        List<Company> companies = companyRepository.findByOwnerId(userId);

        List<CompanyDto> companyDtos = companies.stream()
                .map(CompanyDto::new)
                .collect(Collectors.toList());

        log.info("Found {} companies for userId: {}", companyDtos.size(), userId);

        return ApiResponseDto.success(companyDtos);
    }

    /**
     * Получение данных студента (аналог GetStudent из Go)
     */
    public ApiResponseDto<StudentResponseData> getStudentData(Integer userId) {
        log.debug("Getting student data for userId: {}", userId);

        // Получение данных пользователя
        User user = userRepository.findByIdWithUserData(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Получение данных студента
        Student student = studentRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new RuntimeException("Student data not found"));

        // Получение данных образовательного учреждения
        var educationalInstitution = student.getEducationalInstitution();

        // Создание ответа
        StudentResponseData responseData = new StudentResponseData(
                userMapper.toDto(user),
                new StudentDto(student),
                educationalInstitution != null ? new ru.platik777.backauth.dto.EducationalInstitutionDto(educationalInstitution) : null
        );

        log.info("Retrieved student data for userId: {}", userId);

        return ApiResponseDto.success(responseData);
    }

    /**
     * Редактирование данных пользователя (аналог EditUser из Go)
     */
    public UserDto editUser(Integer userId, UserDto userDto) {
        log.debug("Editing user data for userId: {}", userId);

        // Получение пользователя
        User user = userRepository.findByIdWithUserData(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Валидация изменений
        validateUserChanges(user, userDto);

        // Обновление данных пользователя
        updateUserData(user, userDto);

        // Сохранение изменений
        user = userRepository.save(user);

        log.info("User data updated for userId: {}", userId);

        return userMapper.toDto(user);
    }

    /**
     * Получение полной информации о пользователе
     */
    public UserDto getUserWithFullData(Integer userId) {
        log.debug("Getting full user data for userId: {}", userId);

        User user = userRepository.findByIdWithUserData(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = userMapper.toDto(user);

        // TODO: Добавить роли и разрешения
        // userDto.setRoles(getRolesForUser(userId));
        // userDto.setPermissions(getPermissionsForUser(userId));

        return userDto;
    }

    /**
     * Проверка принадлежности компании пользователю
     */
    public boolean isUserCompanyOwner(Integer userId, Integer companyId) {
        return companyRepository.findById(companyId)
                .map(company -> company.getOwner().getId().equals(userId))
                .orElse(false);
    }

    /**
     * Проверка является ли пользователь студентом
     */
    public boolean isUserStudent(Integer userId) {
        return studentRepository.existsByUserId(userId);
    }

    /**
     * Получение типа аккаунта пользователя
     */
    public User.AccountType getUserAccountType(Integer userId) {
        return userRepository.findById(userId)
                .map(User::getAccountType)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Приватные методы

    private void validateUserChanges(User existingUser, UserDto newUserData) {
        // Проверка уникальности email если он изменился
        if (newUserData.getEmail() != null &&
                !newUserData.getEmail().equals(existingUser.getUserData().getEmail())) {

            // Проверка уникальности нового email
            // TODO: Добавить проверку через UniquenessService
        }

        // Проверка уникальности phone если он изменился
        if (newUserData.getPhone() != null &&
                !newUserData.getPhone().equals(existingUser.getUserData().getPhone())) {

            // Проверка уникальности нового телефона
            // TODO: Добавить проверку через UniquenessService
        }

        // Проверка уникальности login если он изменился
        if (newUserData.getLogin() != null &&
                !newUserData.getLogin().equals(existingUser.getLogin())) {

            // Проверка уникальности нового логина
            // TODO: Добавить проверку через UniquenessService
        }
    }

    private void updateUserData(User user, UserDto userDto) {
        // Обновление основных данных пользователя
        if (userDto.getSettings() != null) {
            user.setSettings(userDto.getSettings());
        }

        // Обновление данных в UserData
        if (user.getUserData() != null) {
            if (userDto.getEmail() != null) {
                user.getUserData().setEmail(userDto.getEmail());
            }
            if (userDto.getUserName() != null) {
                user.getUserData().setUserName(userDto.getUserName());
            }
            if (userDto.getPhone() != null) {
                user.getUserData().setPhone(userDto.getPhone());
            }
        }
    }

    // Внутренние классы для ответов

    /**
     * Класс для ответа с данными студента
     */
    public record StudentResponseData(
            UserDto userData,
            StudentDto studentData,
            ru.platik777.backauth.dto.EducationalInstitutionDto educationalInstitutionData
    ) {}

    /**
     * Класс для ответа со списком компаний
     */
    public record CompaniesResponse(List<CompanyDto> companies) {}
}