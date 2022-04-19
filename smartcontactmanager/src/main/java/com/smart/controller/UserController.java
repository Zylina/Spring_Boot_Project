package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.config.helper.Message;
import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;

@Controller
@RequestMapping("/user")
public class UserController {

	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {

		String userName = principal.getName();
		System.out.println("USERNAME " + userName);

		// get the user using username(Email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER " + user);
		model.addAttribute("user", user);
	}

	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model m) {
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();
			User user = userRepository.getUserByUserName(name);

//		 if(3>2) {
//			 throw new Exception();
//		 }

			// processing and uploading file..

			if (file.isEmpty()) {
				// if the file is Empty then try our message
				System.out.println("File is Empty");
				contact.setImage("contact.png");
			} else {
				// upload the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image is Uploaded");
			}
			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);

			System.out.println("DATA" + contact);
			System.out.println("Added to Database");
			// message success............
			session.setAttribute("message", new Message("Your contact is added Successfully !! Add more..", "success"));

		} catch (Exception e) {
			System.out.println("Error" + e.getMessage());
			e.printStackTrace();
			// error meassage........
			session.setAttribute("message", new Message("something went wrong !! Try again..", "danger"));
		}
		return "normal/add_contact_form";
	}

	// show contacts handler
	// per page =5[n]
	// current page = 0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		// sending title
		model.addAttribute("title", "Show User Contacts");

		// sending contact list
		// the user who is login find it then get all the details using principal

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		// currentPage-page
		// contact Per page - 5
		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/show_contacts";
	}

	// showing particular contact detail
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID" + cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, HttpSession session,Principal principal) {

		Contact contact = this.contactRepository.findById(cId).get();
		System.out.println("Contact " + contact.getcId());
       //delete old photo
		//contact.setUser(null);
         
		// remove
		// img
		// contact.getImage()
        User user = this.userRepository.getUserByUserName(principal.getName());
        user.getContacts().remove(contact);
        this.userRepository.save(user);
		
		System.out.println("DELETED");
		session.setAttribute("message", new Message("Contact deleted successsfully...", "success"));

		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid , Model model) {
		
		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	//update contact  handler
	@RequestMapping(value="/process-update" , method=RequestMethod.POST)
    public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
    		Model model,HttpSession session ,Principal principal) {
		 try {
			 
			 //old contact detail
			 Contact oldcontactdetail = this.contactRepository.findById(contact.getcId()).get();
			 if(!file.isEmpty()) {
				 //file work
				 //rewrite the new file delete old file
				 //delete old photo
				 File deleteFile = new ClassPathResource("static/img").getFile();
				 File file1 = new File(deleteFile , oldcontactdetail.getImage());
				 file1.delete();
				 
				//update new photo
				 File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
					contact.setImage(file.getOriginalFilename());
				 
			 }
			 else {
				 contact.setImage(oldcontactdetail.getImage());
			 }
			 User user = this.userRepository.getUserByUserName(principal.getName());
			 contact.setUser(user);
			 this.contactRepository.save(contact);
			 session.setAttribute("message", new Message("Your contact is updated !!!","success"));
			 
		 }catch(Exception e) {
			 e.printStackTrace();
		 }
		
		System.out.println("CONTACT NAME " +contact.getName());
		System.out.println("CONTACT ID " +contact.getcId());
    	return "redirect:/user/" +contact.getcId()+"/contact";
    }
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	//open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword , @RequestParam("newPassword") String newPassword
			 ,Principal principal, HttpSession session) {
		
		System.out.println("OLD PASSWORD " +oldPassword);
		System.out.println("NEW PASSWORD " +newPassword);
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			//change the password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password is successfully changed !!","success"));
		}else {
			//error...
			session.setAttribute("message", new Message("Please enter correct old password !!","danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
	
}
