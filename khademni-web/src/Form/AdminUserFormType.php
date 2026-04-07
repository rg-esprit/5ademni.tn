<?php

namespace App\Form;

use App\Entity\User;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\PasswordType;
use Symfony\Component\Form\Extension\Core\Type\RepeatedType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\LessThanOrEqual;
use Symfony\Component\Validator\Constraints\NotBlank;

class AdminUserFormType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('firstName', TextType::class, [
                'label' => 'First Name',
                'constraints' => [new NotBlank(), new Length(max: 100)],
            ])
            ->add('lastName', TextType::class, [
                'label' => 'Last Name',
                'constraints' => [new NotBlank(), new Length(max: 100)],
            ])
            ->add('dateOfBirth', DateType::class, [
                'label' => 'Date of Birth',
                'widget' => 'single_text',
                'constraints' => [
                    new NotBlank(),
                    new LessThanOrEqual('today', message: 'Date of birth cannot be in the future.'),
                ],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Email',
                'constraints' => [new NotBlank(), new Email(), new Length(max: 255)],
            ])
            ->add('balance', NumberType::class, [
                'label' => 'Balance',
                'scale' => 2,
                'html5' => true,
                'attr' => [
                    'step' => '0.01',
                ],
            ])
            ->add('isAdmin', CheckboxType::class, [
                'label' => 'Admin access',
                'required' => false,
            ])
            ->add('profileImg', TextType::class, [
                'label' => 'Profile Image URL',
                'required' => false,
                'constraints' => [new Length(max: 500)],
            ])
            ->add('bio', TextareaType::class, [
                'label' => 'Bio',
                'required' => false,
                'attr' => ['rows' => 4],
            ])
            ->add('plainPassword', RepeatedType::class, [
                'type' => PasswordType::class,
                'mapped' => false,
                'required' => !$options['is_edit'],
                'invalid_message' => 'Passwords do not match.',
                'first_options' => [
                    'label' => $options['is_edit'] ? 'New Password' : 'Password',
                    'attr' => ['autocomplete' => 'new-password'],
                ],
                'second_options' => [
                    'label' => $options['is_edit'] ? 'Confirm New Password' : 'Confirm Password',
                    'attr' => ['autocomplete' => 'new-password'],
                ],
            ])
            ->add('clearFaceEmbedding', CheckboxType::class, [
                'label' => 'Clear Face ID data',
                'mapped' => false,
                'required' => false,
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => User::class,
            'is_edit' => false,
        ]);

        $resolver->setAllowedTypes('is_edit', 'bool');
    }
}
