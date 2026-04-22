<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\PasswordType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class LoginFormType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('email', EmailType::class, [
                'label' => 'Email',
                'attr' => [
                    'autocomplete' => 'email',
                    'autofocus' => true,
                    'class' => 'text-input',
                ],
            ])
            ->add('password', PasswordType::class, [
                'label' => 'Password',
                'attr' => [
                    'autocomplete' => 'current-password',
                    'class' => 'text-input',
                ],
                'toggle' => true,
                'button_classes' => ['toggle-password-button', 'auth-toggle-password-button'],
                'toggle_container_classes' => ['toggle-password-container', 'auth-toggle-password-container'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'csrf_protection' => false,
        ]);
    }
}
