package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService{

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;



    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {

            Address address=modelMapper.map(addressDTO,Address.class);


            /// during create, the relationship does not exist yet.
            /// You are creating a brand new association between User and Address.
            /// That’s why both sides must be updated. Before Create Suppose
            /// User exists,Address does not exist
            /// User   Address
            /// (no connection)

            List<Address> addressList= user.getAddresses();//First getting all the address present with user
            addressList.add(address);  //Then adding this new address to the list
            user.setAddresses(addressList); //then setting the addresslist to the user

            address.setUser(user); //Then setting the user with the address beacuse its a  bidirectional mapping so we need to update both the sides
            Address savedAddress=addressRepository.save(address);

            return modelMapper.map(savedAddress,AddressDTO.class);

        }

    @Override
    public List<AddressDTO> getAddress() {
        List<Address> addresses=addressRepository.findAll();
        List<AddressDTO> addressDTOs=addresses.stream()
                .map(address -> modelMapper.map(address,AddressDTO.class))
                .collect(Collectors.toList());
        return addressDTOs;
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("Address","addressId",addressId));
        return modelMapper.map(address,AddressDTO.class);

    }

    @Override
    public List<AddressDTO> getUserAddress(User user) {
        List<Address> addresses=user.getAddresses();
        List<AddressDTO> addressDTOs=addresses.stream()
                .map(address -> modelMapper.map(address,AddressDTO.class))
                .collect(Collectors.toList());
        return addressDTOs;

    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDatabase=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("Address","addressId",addressId));

        addressFromDatabase.setCity(addressDTO.getCity());
        addressFromDatabase.setPincode(addressDTO.getPincode());
        addressFromDatabase.setState(addressDTO.getState());
        addressFromDatabase.setCountry(addressDTO.getCountry());
        addressFromDatabase.setStreet(addressDTO.getStreet());
        addressFromDatabase.setBuildingName(addressDTO.getBuildingName());

        Address updatedAddress=addressRepository.save(addressFromDatabase);

//        User user=addressFromDatabase.getUser();
//        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId));  ///Same object,Same relationship,Different values No collection update needed.
//        user.getAddresses().add(updatedAddress);
//        userRepository.save(user);

        return modelMapper.map(updatedAddress,AddressDTO.class);

    }

    @Override
    public String deleteAddress(Long addressId) {
        Address addressFromDatabase=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("Address","addressId",addressId));

        User user=addressFromDatabase.getUser();//First we fetched the user
        user.getAddresses().removeIf(address -> address.getAddressId().equals(addressId)); //We deleted address from user also to maintain bidirectonal if we only delete the address then user is having still the reference it can cause inconsistency in db Bidirectional synchronization is mainly needed when: relationship changes NOT when only entity attributes change.  so in update we commented the lines
        userRepository.save(user);                                                                 //Object removed entirely Relationship removed Collection must also update from both the side

        addressRepository.delete(addressFromDatabase);
        return "Address deleted successfully with addressId: " + addressId;
    }
}
