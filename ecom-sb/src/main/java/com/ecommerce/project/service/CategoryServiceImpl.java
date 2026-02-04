package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
//    private List<Category> categories=new ArrayList<>();
//    private long nextId=1L;

    @Autowired
    private CategoryRepository  categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories() {
        List<Category> categories=categoryRepository.findAll();
        if(categories.isEmpty())
            throw new APIException("No category created till now");

        List<CategoryDTO> categoryDTOS=categories.stream()
        .map(category -> modelMapper.map(category,CategoryDTO.class))
                .toList();
        CategoryResponse categoryResponse=new CategoryResponse();
        categoryResponse.setContent  (categoryDTOS);
        return categoryResponse;   //the method returns all the categories that exists in the  Database
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category=modelMapper.map(categoryDTO,Category.class);
        Category categoryFromDb=categoryRepository.findByCategoryName(category.getCategoryName());
        if(categoryFromDb!=null)
            throw new APIException("Category with the name"+category.getCategoryName()+"already exists ");

//        category.setCategoryId(nextId++);
        Category savedCategory=categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public String deleteCategory(Long categoryId) {

        Category category=categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","categoryId",categoryId));

        categoryRepository.delete(category);
        return "Category with categoryId " + categoryId + "deleted successfully";
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
         //trying to fetch category by id,getting it as optional

        Category savedCategory=categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","categoryId",categoryId));//with that optional checking if there is any value,if no valure throwing an exception
        Category category=modelMapper.map(categoryDTO,Category.class);

        category.setCategoryId(categoryId);  //we are saving the category into the database
        savedCategory=categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);

       }


    }

