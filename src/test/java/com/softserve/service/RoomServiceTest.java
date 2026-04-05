package com.softserve.service;

import com.softserve.dto.RoomDTO;
import com.softserve.dto.RoomTypeDTO;
import com.softserve.entity.Room;
import com.softserve.entity.RoomType;
import com.softserve.exception.EntityAlreadyExistsException;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.mapper.RoomForScheduleInfoMapper;
import com.softserve.mapper.RoomMapper;
import com.softserve.repository.RoomRepository;
import com.softserve.repository.SortOrderRepository;
import com.softserve.service.impl.RoomServiceImpl;
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
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RoomForScheduleInfoMapper roomForScheduleInfoMapper;

    @Mock
    private SortOrderRepository<Room> sortOrderRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room room;
    private RoomDTO roomDTO;
    private RoomType roomType;
    private RoomTypeDTO roomTypeDTO;

    @BeforeEach
    void setUp() {
        roomType = new RoomType();
        roomType.setId(1L);
        roomType.setDescription("Small auditory");

        room = new Room();
        room.setId(1L);
        room.setName("101");
        room.setType(roomType);
        room.setSortOrder(3);

        roomTypeDTO = new RoomTypeDTO();
        roomTypeDTO.setId(1L);
        roomTypeDTO.setDescription("Small auditory");

        roomDTO = new RoomDTO();
        roomDTO.setId(1L);
        roomDTO.setName("101");
        roomDTO.setType(roomTypeDTO);
    }

    // =========================================================
    @Nested
    class GetByIdTests {

        @Test
        void testGetById_happyPath() {
            // Arrange
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomMapper.convertToDto(room)).thenReturn(roomDTO);
            // Act
            RoomDTO actual = roomService.getById(1L);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(roomDTO);
            verify(roomRepository).findById(1L);
            verify(roomMapper).convertToDto(room);
        }

        @Test
        void testGetById_throwsEntityNotFoundException_whenNotFound() {
            // Arrange
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());
            // Act & Assert
            assertThrows(EntityNotFoundException.class, () -> roomService.getById(99L));
            verify(roomRepository).findById(99L);
        }
    }

    // =========================================================
    @Nested
    class GetAllTests {

        @Test
        void testGetAll_returnsList() {
            // Arrange
            List<Room> rooms = Collections.singletonList(room);
            List<RoomDTO> expected = Collections.singletonList(roomDTO);
            when(roomRepository.getAll()).thenReturn(rooms);
            when(roomMapper.convertToDtoList(rooms)).thenReturn(expected);
            // Act
            List<RoomDTO> actual = roomService.getAll();
            // Assert
            assertThat(actual).hasSameSizeAs(expected).hasSameElementsAs(expected);
            verify(roomRepository).getAll();
            verify(roomMapper).convertToDtoList(rooms);
        }

        @Test
        void testGetAll_returnsEmptyList_whenNoRooms() {
            // Arrange
            when(roomRepository.getAll()).thenReturn(Collections.emptyList());
            when(roomMapper.convertToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());
            // Act
            List<RoomDTO> actual = roomService.getAll();
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
            Room disabledRoom = new Room();
            disabledRoom.setId(2L);
            disabledRoom.setName("202");
            disabledRoom.setDisable(true);

            RoomDTO disabledRoomDTO = new RoomDTO();
            disabledRoomDTO.setId(2L);
            disabledRoomDTO.setName("202");
            disabledRoomDTO.setDisable(true);

            List<Room> rooms = Collections.singletonList(disabledRoom);
            List<RoomDTO> expected = Collections.singletonList(disabledRoomDTO);

            when(roomRepository.getDisabled()).thenReturn(rooms);
            when(roomMapper.convertToDtoList(rooms)).thenReturn(expected);
            // Act
            List<RoomDTO> actual = roomService.getDisabled();
            // Assert
            assertThat(actual).hasSameSizeAs(expected);
            assertThat(actual.get(0).isDisable()).isTrue();
            verify(roomRepository).getDisabled();
        }

        @Test
        void testGetDisabled_returnsEmptyList_whenNoDisabledRooms() {
            // Arrange
            when(roomRepository.getDisabled()).thenReturn(Collections.emptyList());
            when(roomMapper.convertToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());
            // Act
            List<RoomDTO> actual = roomService.getDisabled();
            // Assert
            assertThat(actual).isEmpty();
        }
    }

    // =========================================================
    @Nested
    class GetAllOrderedTests {

        @Test
        void testGetAllOrdered_returnsOrderedList() {
            // Arrange
            Room room2 = new Room();
            room2.setId(2L);
            room2.setName("202");
            room2.setSortOrder(1);
            room.setSortOrder(2);

            RoomDTO roomDTO2 = new RoomDTO();
            roomDTO2.setId(2L);
            roomDTO2.setName("202");

            List<Room> orderedRooms = Arrays.asList(room2, room);
            List<RoomDTO> expected = Arrays.asList(roomDTO2, roomDTO);

            when(roomRepository.getAllOrdered()).thenReturn(orderedRooms);
            when(roomMapper.convertToDtoList(orderedRooms)).thenReturn(expected);
            // Act
            List<RoomDTO> actual = roomService.getAllOrdered();
            // Assert
            assertThat(actual).hasSize(2);
            assertThat(actual.get(0).getId()).isEqualTo(2L);
            assertThat(actual.get(1).getId()).isEqualTo(1L);
            verify(roomRepository).getAllOrdered();
        }
    }

    // =========================================================
    @Nested
    class SaveTests {

        @Test
        void testSave_happyPath() {
            // Arrange
            when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
            when(roomRepository.countRoomDuplicates(room)).thenReturn(0L);
            when(roomRepository.getLastSortOrder()).thenReturn(Optional.of(5));
            when(roomRepository.save(room)).thenReturn(room);
            when(roomMapper.convertToDto(room)).thenReturn(roomDTO);
            // Act
            RoomDTO actual = roomService.save(roomDTO);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(roomDTO);
            verify(roomRepository).countRoomDuplicates(room);
            verify(roomRepository).save(room);
        }

        @Test
        void testSave_throwsEntityAlreadyExistsException_whenDuplicate() {
            // Arrange
            when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
            when(roomRepository.countRoomDuplicates(room)).thenReturn(1L);
            // Act & Assert
            assertThrows(EntityAlreadyExistsException.class, () -> roomService.save(roomDTO));
            verify(roomRepository, never()).save(any());
        }
    }

    // =========================================================
    @Nested
    class UpdateTests {

        @Test
        void testUpdate_happyPath() {
            // Arrange
            when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
            when(roomRepository.countRoomDuplicates(room)).thenReturn(0L);
            when(sortOrderRepository.getSortOrderById(room.getId())).thenReturn(Optional.of(3));
            when(roomRepository.update(room)).thenReturn(room);
            when(roomMapper.convertToDto(room)).thenReturn(roomDTO);
            // Act
            RoomDTO actual = roomService.update(roomDTO);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(roomDTO);
            verify(roomRepository).countRoomDuplicates(room);
            verify(roomRepository).update(room);
        }

        @Test
        void testUpdate_throwsEntityAlreadyExistsException_whenDuplicate() {
            // Arrange
            when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
            when(roomRepository.countRoomDuplicates(room)).thenReturn(1L);
            // Act & Assert
            assertThrows(EntityAlreadyExistsException.class, () -> roomService.update(roomDTO));
            verify(roomRepository, never()).update(any());
        }
    }

    // =========================================================
    @Nested
    class DeleteByIdTests {

        @Test
        void testDeleteById_happyPath_andCallsShiftSortOrderRange() {
            // Arrange
            room.setSortOrder(3);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomRepository.delete(room)).thenReturn(room);
            when(roomMapper.convertToDto(room)).thenReturn(roomDTO);
            // Act
            RoomDTO actual = roomService.deleteById(1L);
            // Assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(roomDTO);
            verify(roomRepository).findById(1L);
            verify(roomRepository).delete(room);
            // sortOrder was 3, so shift starts from 4 (3+1), upperBound null, direction UP
            verify(roomRepository).shiftSortOrderRange(4, null, RoomRepository.Direction.UP);
        }

        @Test
        void testDeleteById_throwsEntityNotFoundException_whenNotFound() {
            // Arrange
            when(roomRepository.findById(99L)).thenReturn(Optional.empty());
            // Act & Assert
            assertThrows(EntityNotFoundException.class, () -> roomService.deleteById(99L));
            verify(roomRepository, never()).delete(any());
            verify(roomRepository, never()).shiftSortOrderRange(any(), any(), any());
        }
    }
}
