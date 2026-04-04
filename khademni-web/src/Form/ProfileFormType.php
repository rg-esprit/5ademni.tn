<?php

namespace App\Form;

use App\Entity\User;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;

class ProfileFormType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('firstName', TextType::class, [
                'label' => 'First Name',
                'constraints' => [new NotBlank()],
            ])
            ->add('lastName', TextType::class, [
                'label' => 'Last Name',
                'constraints' => [new NotBlank()],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Email',
                'constraints' => [new NotBlank(), new Email()],
            ])
            ->add('dateOfBirth', DateType::class, [
                'label' => 'Date of Birth',
                'widget' => 'single_text',
                'constraints' => [new NotBlank()],
            ])
            ->add('bio', TextareaType::class, [
                'label' => 'Bio',
                'required' => false,
                'attr' => ['rows' => 4],
            ])
            ->add('profileImageFile', FileType::class, [
                'label' => 'Profile Image',
                'mapped' => false,
                'required' => false,
                'constraints' => [
                    new File(maxSize: '5M', mimeTypes: ['image/png', 'image/jpeg', 'image/webp', 'image/gif']),
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => User::class,
        ]);
    }
}
