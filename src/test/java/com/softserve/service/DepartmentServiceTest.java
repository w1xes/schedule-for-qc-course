package com.softserve.service;

import com.softserve.dto.DepartmentDTO;
import com.softserve.dto.TeacherDTO;
import com.softserve.entity.Department;
import com.softserve.entity.Teacher;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.exception.FieldAlreadyExistsException;
import com.softserve.mapper.DepartmentMapper;
import com.softserve.mapper.TeacherMapper;
import com.softserve.repository.DepartmentRepository;
import com.softserve.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository repository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private TeacherMapper teacherMapper;

    @InjectMocks
    private DepartmentServiceImpl service;

    private Department department;
    private DepartmentDTO departmentDTO;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("some department");

        departmentDTO = new DepartmentDTO();
        departmentDTO.setId(1L);
        departmentDTO.setName("some department");
    }

    // =========================================================
    @Nested
    class GetByIdTests {

        @Test
        void testGetById_happyPath() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(departmentMapper.departmentToDepartmentDTO(department)).thenReturn(departmentDTO);
            // Act
            DepartmentDTO actual = service.getById(1L);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(departmentDTO);
            verify(repository).findById(1L);
            verify(departmentMapper).departmentToDepartmentDTO(department);
        }

        @Test
        void testGetById_throwsEntityNotFoundException_whenNotFound() {
            // Arrange
            Long nonExistentId = 2L;
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());
            // Act & Assert
            assertThrows(EntityNotFoundException.class, () -> service.getById(nonExistentId));
            verify(repository).findById(nonExistentId);
        }
    }

    // =========================================================
    @Nested
    class GetAllTests {

        @Test
        void testGetAll_returnsList() {
            // Arrange
            List<Department> departments = Collections.singletonList(department);
            List<DepartmentDTO> expected = Collections.singletonList(departmentDTO);
            when(repository.getAll()).thenReturn(departments);
            when(departmentMapper.departmentsToDepartmentDTOs(departments)).thenReturn(expected);
            // Act
            List<DepartmentDTO> actual = service.getAll();
            // Assert
            assertThat(actual).hasSameSizeAs(expected).hasSameElementsAs(expected);
            verify(repository).getAll();
            verify(departmentMapper).departmentsToDepartmentDTOs(departments);
        }

        @Test
        void testGetAll_returnsEmptyList_whenNoDepartments() {
            // Arrange
            when(repository.getAll()).thenReturn(Collections.emptyList());
            when(departmentMapper.departmentsToDepartmentDTOs(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());
            // Act
            List<DepartmentDTO> actual = service.getAll();
            // Assert
            assertThat(actual).isEmpty();
        }
    }

    // =========================================================
    @Nested
    class GetDisabledTests {

        @Test
        void testGetDisabled_returnsList() {
            // Arrange
            List<Department> departments = Collections.singletonList(department);
            List<DepartmentDTO> expected = Collections.singletonList(departmentDTO);
            when(repository.getDisabled()).thenReturn(departments);
            when(departmentMapper.departmentsToDepartmentDTOs(departments)).thenReturn(expected);
            // Act
            List<DepartmentDTO> actual = service.getDisabled();
            // Assert
            assertThat(actual).hasSameSizeAs(expected).hasSameElementsAs(expected);
            verify(repository).getDisabled();
            verify(departmentMapper).departmentsToDepartmentDTOs(departments);
        }
    }

    // =========================================================
    @Nested
    class SaveTests {

        @Test
        void testSave_happyPath() {
            // Arrange
            when(departmentMapper.departmentDTOToDepartment(departmentDTO)).thenReturn(department);
            when(repository.isExistsByName(department.getName())).thenReturn(false);
            when(repository.save(department)).thenReturn(department);
            when(departmentMapper.departmentToDepartmentDTO(department)).thenReturn(departmentDTO);
            // Act
            DepartmentDTO actual = service.save(departmentDTO);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(departmentDTO);
            verify(departmentMapper).departmentDTOToDepartment(departmentDTO);
            verify(repository).isExistsByName(department.getName());
            verify(repository).save(department);
            verify(departmentMapper).departmentToDepartmentDTO(department);
        }

        @Test
        void testSave_throwsFieldAlreadyExistsException_whenNameDuplicated() {
            // Arrange
            when(departmentMapper.departmentDTOToDepartment(departmentDTO)).thenReturn(department);
            when(repository.isExistsByName(department.getName())).thenReturn(true);
            // Act & Assert
            assertThrows(FieldAlreadyExistsException.class, () -> service.save(departmentDTO));
            verify(departmentMapper).departmentDTOToDepartment(departmentDTO);
            verify(repository).isExistsByName(department.getName());
            verify(repository, never()).save(any());
        }
    }

    // =========================================================
    @Nested
    class UpdateTests {

        @Test
        void testUpdate_happyPath() {
            // Arrange
            when(departmentMapper.departmentDTOToDepartment(departmentDTO)).thenReturn(department);
            when(repository.isExistsByNameIgnoringId(department.getName(), department.getId())).thenReturn(false);
            when(repository.update(department)).thenReturn(department);
            when(departmentMapper.departmentToDepartmentDTO(department)).thenReturn(departmentDTO);
            // Act
            DepartmentDTO actual = service.update(departmentDTO);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(departmentDTO);
            verify(departmentMapper).departmentDTOToDepartment(departmentDTO);
            verify(repository).isExistsByNameIgnoringId(department.getName(), department.getId());
            verify(repository).update(department);
            verify(departmentMapper).departmentToDepartmentDTO(department);
        }

        @Test
        void testUpdate_throwsFieldAlreadyExistsException_whenNameDuplicated() {
            // Arrange
            when(departmentMapper.departmentDTOToDepartment(departmentDTO)).thenReturn(department);
            when(repository.isExistsByNameIgnoringId(department.getName(), department.getId())).thenReturn(true);
            // Act & Assert
            assertThrows(FieldAlreadyExistsException.class, () -> service.update(departmentDTO));
            verify(departmentMapper).departmentDTOToDepartment(departmentDTO);
            verify(repository).isExistsByNameIgnoringId(department.getName(), department.getId());
            verify(repository, never()).update(any());
        }
    }

    // =========================================================
    @Nested
    class DeleteTests {

        @Test
        void testDelete_happyPath() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(department));
            when(repository.delete(department)).thenReturn(department);
            when(departmentMapper.departmentToDepartmentDTO(department)).thenReturn(departmentDTO);
            // Act
            DepartmentDTO actual = service.delete(1L);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(departmentDTO);
            verify(repository).findById(1L);
            verify(repository).delete(department);
            verify(departmentMapper).departmentToDepartmentDTO(department);
        }

        @Test
        void testDelete_throwsEntityNotFoundException_whenNotFound() {
            // Arrange
            Long nonExistentId = 99L;
            when(repository.findById(nonExistentId)).thenReturn(Optional.empty());
            // Act & Assert
            assertThrows(EntityNotFoundException.class, () -> service.delete(nonExistentId));
            verify(repository).findById(nonExistentId);
            verify(repository, never()).delete(any());
        }
    }

    // =========================================================
    @Nested
    class GetAllTeachersTests {

        @Test
        void testGetAllTeachers_returnsList() {
            // Arrange
            Teacher firstTeacher = new Teacher();
            firstTeacher.setName("Myroniuk");
            firstTeacher.setSurname("Ihor");
            firstTeacher.setPatronymic("Stepanovych");
            firstTeacher.setPosition("professor");

            Teacher secondTeacher = new Teacher();
            secondTeacher.setName("Adamovych");
            secondTeacher.setSurname("Svitlana");
            secondTeacher.setPatronymic("Petrivna");
            secondTeacher.setPosition("docent");

            List<Teacher> teachers = Arrays.asList(firstTeacher, secondTeacher);

            TeacherDTO firstTeacherDTO = new TeacherDTO();
            firstTeacherDTO.setName("Myroniuk");
            firstTeacherDTO.setSurname("Ihor");
            firstTeacherDTO.setPatronymic("Stepanovych");
            firstTeacherDTO.setPosition("professor");

            TeacherDTO secondTeacherDTO = new TeacherDTO();
            secondTeacherDTO.setName("Adamovych");
            secondTeacherDTO.setSurname("Svitlana");
            secondTeacherDTO.setPatronymic("Petrivna");
            secondTeacherDTO.setPosition("docent");

            List<TeacherDTO> expected = Arrays.asList(firstTeacherDTO, secondTeacherDTO);

            when(repository.getAllTeachers(3L)).thenReturn(teachers);
            when(teacherMapper.teachersToTeacherDTOs(teachers)).thenReturn(expected);
            // Act
            List<TeacherDTO> actual = service.getAllTeachers(3L);
            // Assert
            assertThat(actual).hasSameSizeAs(expected).hasSameElementsAs(expected);
            verify(repository).getAllTeachers(3L);
            verify(teacherMapper).teachersToTeacherDTOs(teachers);
        }

        @Test
        void testGetAllTeachers_returnsEmptyList_whenDepartmentHasNoTeachers() {
            // Arrange
            when(repository.getAllTeachers(1L)).thenReturn(Collections.emptyList());
            when(teacherMapper.teachersToTeacherDTOs(Collections.emptyList())).thenReturn(Collections.emptyList());
            // Act
            List<TeacherDTO> actual = service.getAllTeachers(1L);
            // Assert
            assertThat(actual).isEmpty();
            verify(repository).getAllTeachers(1L);
        }
    }
}
